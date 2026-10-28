package com.tallermecanico.vehicle;

import com.tallermecanico.workshop.client;
import com.tallermecanico.workshop.client_repository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Implementa alta, búsqueda, detalle, detección duplicada y desactivación lógica de vehículos. */
@Service
public class vehicle_service {
    /** Límite individual solicitado para cada fotografía, convertido a bytes. */
    private static final long MAX_PHOTO_BYTES=15L*1024*1024;
    /** Repositorio de vehículos con constraint único persistido para cada identificador. */
    private final vehicle_repository vehicles;
    /** Repositorio de clientes que comprueba la precondición de cuenta activa. */
    private final client_repository clients;
    /** Factory Method que concentra el ensamblado del agregado del dominio. */
    private final vehicle_factory factory;
    /** Strategy que normaliza y valida el formato de placas mexicanas. */
    private final plate_normalization_strategy plate_strategy;
    /** Directorio persistente montado sobre el volumen Docker de archivos. */
    private final Path upload_dir;

    /** Inyecta persistencia, reglas de normalización, fábrica de dominio y ruta del volumen persistente. */
    public vehicle_service(vehicle_repository vehicles,client_repository clients,vehicle_factory factory,
                           plate_normalization_strategy plate_strategy,
                           @Value("${app.workshop.upload-dir}") String upload_dir) {
        this.vehicles=vehicles;
        this.clients=clients;
        this.factory=factory;
        this.plate_strategy=plate_strategy;
        this.upload_dir=Path.of(upload_dir).resolve("vehicles");
    }

    /** Devuelve solo vehículos activos en páginas y conserva el orden indicado por el controlador. */
    @Transactional(readOnly=true)
    public vehicle_page_response list(String term,Pageable pageable) {
        Page<vehicle> page=vehicles.search_active(term==null?"":term.trim(),vehicle_status.activo,pageable);
        return new vehicle_page_response(page.getContent().stream().map(this::to_response).toList(),page.getNumber(),page.getSize(),page.getTotalElements(),page.getTotalPages());
    }

    /** Consulta la ficha activa y sus metadatos del propietario y fotografías. */
    @Transactional(readOnly=true)
    public vehicle_response detail(String id) { return to_response(find_active(id)); }

    /** Detecta por separado VIN y placa repetidos después de normalizar ambos identificadores. */
    @Transactional(readOnly=true)
    public vehicle_duplicate_response duplicates(String raw_vin,String raw_plate) {
        String vin=normalize_vin(raw_vin);
        String plate=plate_strategy.normalize(raw_plate);
        return new vehicle_duplicate_response(vehicles.existsByVin(vin),vehicles.existsByPlate(plate));
    }

    /** Valida cliente y fotos, normaliza identificadores, guarda archivos y crea un vehículo activo. */
    @Transactional
    public vehicle_response create(vehicle_request request,List<MultipartFile> uploads) {
        String vin=normalize_vin(request.vin());
        String plate=plate_strategy.normalize(request.plate());
        if(vehicles.existsByVin(vin)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Ya existe un vehículo registrado con ese VIN.");
        if(vehicles.existsByPlate(plate)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Ya existe un vehículo registrado con esa placa.");
        client owner=clients.findActiveForVehicleRegistration(request.client_id()).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"El cliente seleccionado no existe o está inactivo."));
        List<MultipartFile> files=uploads==null?List.of():uploads.stream().filter(file->file!=null&&!file.isEmpty()).toList();
        List<Path> written=new ArrayList<>();
        try {
            List<vehicle_photo> photos=store_photos(files,written);
            vehicle created=factory.create_vehicle(request,owner,vin,plate,photos);
            return to_response(vehicles.saveAndFlush(created));
        } catch(DataIntegrityViolationException duplicate) {
            delete_files(written);
            throw new ResponseStatusException(HttpStatus.CONFLICT,"El VIN o la placa ya fueron registrados.");
        } catch(Exception failure) {
            delete_files(written);
            if(failure instanceof ResponseStatusException status) throw status;
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"No se pudo completar el registro del vehículo.");
        }
    }

    /** Desactiva el registro sin borrar físicamente ni cliente asociado ni metadatos o fotos. */
    @Transactional
    public void deactivate(String id) {
        vehicle existing=find_active(id);
        existing.deactivate();
        vehicles.save(existing);
    }

    /** Devuelve un recurso solo si la foto pertenece al vehículo activo solicitado. */
    @Transactional(readOnly=true)
    public photo_file photo_file(String vehicle_id,String photo_id) {
        vehicle existing=find_active(vehicle_id);
        vehicle_photo photo=existing.getPhotos().stream().filter(item->item.getId().equals(photo_id)).findFirst()
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"No existe la fotografía solicitada."));
        Path path=upload_dir.resolve(Path.of(photo.getStorage_path()).getFileName()).normalize();
        if(!path.startsWith(upload_dir.normalize())||!Files.isRegularFile(path)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"No existe el archivo solicitado.");
        return new photo_file(new FileSystemResource(path),photo.getContent_type(),photo.getFile_name());
    }

    /** Valida y escribe cada imagen por separado para respetar el máximo individual de 15 MB. */
    private List<vehicle_photo> store_photos(List<MultipartFile> uploads,List<Path> written) throws IOException {
        List<vehicle_photo> photos=new ArrayList<>();
        Files.createDirectories(upload_dir);
        for(MultipartFile upload:uploads) {
            if(upload.getSize()>MAX_PHOTO_BYTES) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Cada fotografía debe pesar como máximo 15 MB.");
            String mime=upload.getContentType()==null?"":upload.getContentType().toLowerCase(Locale.ROOT);
            if(!List.of("image/jpeg","image/png","image/gif","image/bmp").contains(mime)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Solo se permiten fotografías JPEG, PNG, GIF o BMP.");
            try(var image_stream=upload.getInputStream()) {
                if(ImageIO.read(image_stream)==null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Uno de los archivos no contiene una imagen válida.");
            }
            String extension=extension(mime);
            String generated_name=UUID.randomUUID()+extension;
            Path destination=upload_dir.resolve(generated_name).normalize();
            if(!destination.startsWith(upload_dir.normalize())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Nombre de archivo no válido.");
            upload.transferTo(destination);
            written.add(destination);
            String original=safe_file_name(upload.getOriginalFilename(),extension);
            photos.add(new vehicle_photo(original,"vehicles/"+generated_name,mime,upload.getSize()));
        }
        return photos;
    }

    /** Reduce el VIN a mayúsculas y vuelve a validar sus 17 símbolos permitidos. */
    private String normalize_vin(String raw_vin) {
        String normalized=raw_vin.trim().toUpperCase(Locale.ROOT);
        if(!normalized.matches("^[A-HJ-NPR-Z0-9]{17}$")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El VIN debe tener 17 caracteres y no puede incluir I, O o Q.");
        return normalized;
    }

    /** Devuelve una extensión controlada por la lista blanca de MIME validado. */
    private String extension(String mime) {
        return switch(mime) { case "image/jpeg" -> ".jpg"; case "image/png" -> ".png"; case "image/gif" -> ".gif"; default -> ".bmp"; };
    }

    /** Sanea el nombre original para impedir rutas, controles y nombres mayores al límite SQL. */
    private String safe_file_name(String raw_name,String extension) {
        String name=raw_name==null?"":raw_name.replace('\\','/');
        int separator=name.lastIndexOf('/');
        if(separator>=0) name=name.substring(separator+1);
        String safe=name.replaceAll("[^A-Za-z0-9._ -]","_").trim();
        return safe.isBlank()?"foto"+extension:safe.substring(0,Math.min(safe.length(),255));
    }

    /** Borra archivos nuevos si falla la persistencia para que no queden archivos huérfanos. */
    private void delete_files(List<Path> files) { for(Path file:files) try { Files.deleteIfExists(file); } catch(IOException ignored) { /* La excepción original conserva el resultado del caso de uso. */ } }

    /** Recupera exclusivamente un vehículo activo para consulta, foto o desactivación. */
    private vehicle find_active(String id) {
        return vehicles.findByIdAndStatus(id,vehicle_status.activo).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"No existe un vehículo activo con ese identificador."));
    }

    /** Proyecta entidad, cliente y fotos a DTOs seguros mientras la transacción mantiene las relaciones cargadas. */
    private vehicle_response to_response(vehicle value) {
        client owner=value.getOwner();
        vehicle_response.client_summary client_view=new vehicle_response.client_summary(owner.getId(),owner.getName(),owner.getPaternal_surname(),owner.getMaternal_surname(),owner.getEmail());
        List<vehicle_response.photo_summary> photo_views=value.getPhotos().stream().map(photo->new vehicle_response.photo_summary(photo.getId(),photo.getFile_name(),photo.getContent_type(),photo.getSize_bytes(),"/api/vehicles/"+value.getId()+"/photos/"+photo.getId())).toList();
        String mechanic_id=value.getMechanic()==null?null:value.getMechanic().getId();
        return new vehicle_response(value.getId(),value.getFecha(),value.getVin(),value.getPlate(),value.getMake(),value.getModel(),value.getYear(),value.getVersion(),value.getColor(),value.getMileage(),value.getStatus().name(),mechanic_id,client_view,photo_views);
    }

    /** Empaqueta los datos necesarios para responder a una solicitud autenticada de fotografía. */
    public record photo_file(Resource resource,String content_type,String file_name) {}
}
