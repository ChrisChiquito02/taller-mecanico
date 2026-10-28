package com.tallermecanico.vehicle;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Expone rutas protegidas para registrar, consultar, desactivar y visualizar fotos de vehículos. */
@Validated
@RestController
@RequestMapping("/api/vehicles")
public class vehicle_controller {
    /** Caso de uso que contiene validación, asociación, persistencia y manejo de archivos. */
    private final vehicle_service service;

    /** Inyecta el servicio de aplicación que opera el agregado de vehículos. */
    public vehicle_controller(vehicle_service service) { this.service=service; }

    /** Devuelve una página de vehículos activos con búsqueda y orden cronológico seleccionable. */
    @GetMapping
    public vehicle_page_response list(@RequestParam(defaultValue="0") @Min(0) int page,
                                     @RequestParam(defaultValue="10") @Min(1) @Max(10) int size,
                                     @RequestParam(defaultValue="") @Size(max=120) String term,
                                     @RequestParam(defaultValue="DESC") @Pattern(regexp="(?i)ASC|DESC") String direction) {
        Sort.Direction order=Sort.Direction.fromString(direction);
        return service.list(term,PageRequest.of(page,size,Sort.by(order,"fecha").and(Sort.by(order,"id"))));
    }

    /** Revisa en servidor los duplicados normalizados antes de que el formulario envíe el alta. */
    @GetMapping("/duplicates")
    public vehicle_duplicate_response duplicates(@RequestParam @Pattern(regexp="(?i)^[A-HJ-NPR-Z0-9]{17}$") String vin,
                                                 @RequestParam @Size(min=5,max=10) @Pattern(regexp="^[A-Za-z0-9 -]+$") String plate) {
        return service.duplicates(vin,plate);
    }

    /** Registra un vehículo con cliente activo y cero o más fotografías multipart validadas. */
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public vehicle_response create(@Valid @RequestPart("data") vehicle_request request,
                                   @RequestPart(value="photos",required=false) List<MultipartFile> photos) {
        return service.create(request,photos);
    }

    /** Consulta el detalle de un vehículo activo y sus datos de cliente, mecánico y fotos. */
    @GetMapping("/{id}")
    public vehicle_response detail(@PathVariable @Pattern(regexp="^[0-9a-fA-F-]{36}$") String id) {
        return service.detail(id);
    }

    /** Entrega una imagen del vehículo solo a un usuario autorizado y con contenido tipo inline. */
    @GetMapping("/{id}/photos/{photo_id}")
    public ResponseEntity<Resource> photo(@PathVariable @Pattern(regexp="^[0-9a-fA-F-]{36}$") String id,
                                          @PathVariable @Pattern(regexp="^[0-9a-fA-F-]{36}$") String photo_id) {
        vehicle_service.photo_file file=service.photo_file(id,photo_id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.content_type()))
                .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.inline().filename(file.file_name()).build().toString())
                .body(file.resource());
    }

    /** Desactiva un vehículo con borrado lógico y conserva su histórico en MySQL. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable @Pattern(regexp="^[0-9a-fA-F-]{36}$") String id) { service.deactivate(id); }
}
