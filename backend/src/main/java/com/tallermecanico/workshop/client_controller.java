package com.tallermecanico.workshop;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Expone las operaciones protegidas para registrar y administrar clientes. */
@Validated
@RestController
@RequestMapping("/api/clients")
public class client_controller {
    private final client_service service;

    /** Inyecta el caso de uso transaccional de clientes. */
    public client_controller(client_service service) { this.service = service; }

    /** Consulta clientes activos con paginación de servidor y búsqueda por nombre o correo. */
    @GetMapping
    public client_page_response list(@RequestParam(defaultValue="0") @Min(0) int page,
                                     @RequestParam(defaultValue="10") @Min(1) @Max(10) int size,
                                     @RequestParam(defaultValue="") String term,
                                     @RequestParam(defaultValue="DESC") @Pattern(regexp="(?i)ASC|DESC") String direction) {
        Sort.Direction order = Sort.Direction.fromString(direction);
        Sort sort = Sort.by(order, "fecha").and(Sort.by(order, "id"));
        return service.list(term, PageRequest.of(page, size, sort));
    }

    /** Registra un cliente con al menos un taller previamente registrado. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public client_response create(@Valid @RequestBody client_request request) { return service.create(request); }

    /** Actualiza datos de un cliente activo y sus asociaciones obligatorias de taller. */
    @PutMapping("/{id}")
    public client_response update(@PathVariable @Pattern(regexp="^[0-9a-fA-F-]{36}$") String id,
                                 @Valid @RequestBody client_request request) { return service.update(id, request); }

    /** Suspende un cliente sin borrarlo físicamente ni mostrarlo en el listado activo. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable @Pattern(regexp="^[0-9a-fA-F-]{36}$") String id) { service.deactivate(id); }
}
