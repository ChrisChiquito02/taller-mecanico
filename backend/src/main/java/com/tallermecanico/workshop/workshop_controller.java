package com.tallermecanico.workshop;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** API protegida para el alta del taller y catálogos de dirección requeridos por su pantalla. */
@Validated
@RestController
@RequestMapping("/api/workshops")
public class workshop_controller {
    private final workshop_service service; private final postalkit_client postal;
    /** Inyecta el caso de uso de talleres y el proxy del catálogo postal. */
    public workshop_controller(workshop_service service, postalkit_client postal){this.service=service;this.postal=postal;}
    /** Lista talleres disponibles para elegir una asociación durante el registro de clientes. */
    @GetMapping("/catalog") public java.util.List<workshop_summary> catalog(){return service.list_catalog();}
    /** Consulta RFC duplicado antes del envío de alta. */
    @GetMapping("/exists") public exists_response exists(@RequestParam @Pattern(regexp="^[A-Za-zÑ&]{3,4}[0-9]{6}[A-Za-z0-9]{3}$") String rfc){return new exists_response(service.exists_by_rfc(rfc));}
    /** Devuelve estados mexicanos desde PostalKit. */
    @GetMapping("/catalog/states") public JsonNode states(){return postal.states();}
    /** Devuelve municipios para el estado seleccionado. */
    @GetMapping("/catalog/states/{state_id}/municipalities") public JsonNode municipalities(@PathVariable @Pattern(regexp="^[A-Za-z0-9-]{1,80}$") String state_id){return postal.municipalities(state_id);}
    /** Completa estado, municipio y colonias usando el código postal capturado. */
    @GetMapping("/catalog/postal-codes/{code}") public JsonNode postal_code(@PathVariable @Pattern(regexp="^[0-9]{5}$") String code){return postal.postal_code(code);}
    /** Busca el código postal correspondiente a estado, municipio y colonia elegidos. */
    @GetMapping("/catalog/postal-codes/search") public JsonNode postal_search(@RequestParam String state,@RequestParam String municipality,@RequestParam String colonia){return postal.postal_search(state,municipality,colonia);}
    /** Recibe formulario e imagen, aplica validación Bean Validation y ejecuta el alta. */
    @PostMapping(consumes="multipart/form-data") @ResponseStatus(HttpStatus.CREATED)
    public workshop_response create(@Valid @RequestPart("data") create_request data,@RequestPart("photo") MultipartFile photo,@RequestHeader(value="Idempotency-Key",required=false) String idempotency_key){return service.create(data,photo,idempotency_key);}
    /** Forma de respuesta de la consulta previa de RFC. */
    public record exists_response(boolean exists) {}
}
