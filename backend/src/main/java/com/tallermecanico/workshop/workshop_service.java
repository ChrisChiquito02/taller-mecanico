package com.tallermecanico.workshop;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Caso de uso transaccional para validar duplicados, guardar foto y dar de alta un taller. */
@Service
public class workshop_service {
    private static final long max_photo_bytes=15L*1024*1024;
    private final workshop_repository workshops; private final workshop_factory factory; private final ObjectMapper mapper; private final Path upload_dir;
    /** Recibe persistencia, Factory Method, serializador y ruta persistente de imágenes. */
    public workshop_service(workshop_repository workshops, workshop_factory factory, ObjectMapper mapper, @Value("${app.workshop.upload-dir}") String upload_dir){this.workshops=workshops;this.factory=factory;this.mapper=mapper;this.upload_dir=Path.of(upload_dir);}
    /** Proyecta los talleres persistidos para que el selector no exponga entidades JPA. */
    @Transactional(readOnly=true) public java.util.List<workshop_summary> list_catalog(){return workshops.findAllByOrderByFechaDesc().stream().map(item->new workshop_summary(item.getId(),item.getName(),item.getRfc(),item.getCommercial_status().name())).toList();}
    /** Consulta duplicados por RFC antes de iniciar el registro. */
    @Transactional(readOnly=true) public boolean exists_by_rfc(String rfc){return workshops.existsByRfc(rfc.trim().toUpperCase(Locale.ROOT));}
    /** Valida archivo, idempotencia y RFC y persiste el taller creado por Factory Method. */
    @Transactional public workshop_response create(create_request request, MultipartFile photo, String idempotency_key){
        byte[] bytes=validate_photo(photo); String key=idempotency_key==null?"":idempotency_key.trim();
        if(!key.matches("^[A-Za-z0-9-]{16,80}$"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La clave de idempotencia es obligatoria y no válida.");
        String digest=digest(request,bytes);
        var prior=workshops.find_idempotency_key(key);
        if(prior.isPresent()){if(!prior.get().getRequest_hash().equals(digest))throw new ResponseStatusException(HttpStatus.CONFLICT,"La clave de idempotencia ya se utilizó con datos distintos.");return workshop_response.from(prior.get());}
        String rfc=request.rfc().trim().toUpperCase(Locale.ROOT);
        if(workshops.existsByRfc(rfc))throw new ResponseStatusException(HttpStatus.CONFLICT,"Ya existe un taller registrado con ese RFC.");
        String photo_name=java.util.UUID.randomUUID()+extension(photo.getContentType()); Path destination=upload_dir.resolve(photo_name);
        try{
            Files.createDirectories(upload_dir); Files.write(destination,bytes);
            workshop saved=workshops.saveAndFlush(factory.create_workshop(request,"/uploads/"+photo_name,key,digest));
            return workshop_response.from(saved);
        }catch(DataIntegrityViolationException duplicate){delete_photo(destination);throw new ResponseStatusException(HttpStatus.CONFLICT,"El RFC o la clave de idempotencia ya fueron registrados.");}
        catch(Exception error){delete_photo(destination);if(error instanceof ResponseStatusException status)throw status;throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"No se pudo completar el registro del taller.");}
    }
    /** Valida tipo MIME, tamaño máximo y contenido de imagen antes de usar los bytes. */
    private byte[] validate_photo(MultipartFile photo){
        if(photo==null||photo.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La fotografía del taller es obligatoria.");
        if(photo.getSize()>max_photo_bytes)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La fotografía no puede superar 15 MB.");
        String mime=photo.getContentType()==null?"":photo.getContentType().toLowerCase(Locale.ROOT);
        if(!java.util.Set.of("image/jpeg","image/png","image/gif","image/bmp").contains(mime))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Solo se permiten imágenes JPEG, PNG, GIF o BMP.");
        try{byte[] bytes=photo.getBytes();if(ImageIO.read(new ByteArrayInputStream(bytes))==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El archivo no contiene una imagen válida.");return bytes;}
        catch(java.io.IOException error){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"No se pudo leer la fotografía.");}
    }
    /** Calcula huella SHA-256 de todos los campos y bytes para garantizar idempotencia exacta. */
    private String digest(create_request request, byte[] bytes){try{MessageDigest sha=MessageDigest.getInstance("SHA-256");sha.update(mapper.writeValueAsBytes(request));sha.update(bytes);return HexFormat.of().formatHex(sha.digest());}catch(Exception error){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"No se pudo validar la idempotencia.");}}
    /** Devuelve una extensión permitida derivada del MIME real validado. */
    private String extension(String mime){return switch(mime.toLowerCase(Locale.ROOT)){case "image/jpeg"->".jpg";case "image/png"->".png";case "image/gif"->".gif";default->".bmp";};}
    /** Elimina una foto recién escrita cuando la transacción de base de datos falla. */
    private void delete_photo(Path path){try{Files.deleteIfExists(path);}catch(Exception ignored){}}
}
