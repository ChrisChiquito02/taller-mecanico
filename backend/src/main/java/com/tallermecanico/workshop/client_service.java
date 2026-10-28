package com.tallermecanico.workshop;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Ejecuta registro, listado paginado, actualización y suspensión lógica de clientes. */
@Service
public class client_service {
    private final client_repository clients;
    private final workshop_repository workshops;

    /** Recibe repositorios para guardar clientes y sus relaciones con talleres existentes. */
    public client_service(client_repository clients, workshop_repository workshops) {
        this.clients = clients;
        this.workshops = workshops;
    }

    /** Devuelve la página solicitada de clientes activos convertida a respuestas seguras. */
    @Transactional(readOnly = true)
    public client_page_response list(String term, Pageable pageable) {
        Page<client> result = clients.searchActive(term == null ? "" : term.trim(), pageable);
        return new client_page_response(result.getContent().stream().map(this::to_response).toList(), result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    /** Crea el cliente después de normalizar datos, comprobar duplicidad y enlazar talleres existentes. */
    @Transactional
    public client_response create(client_request request) {
        validate_phones(request);
        validate_duplicate(request, null);
        List<workshop> selected = resolve_workshops(request.workshop_ids());
        client created = clients.save(new client(request));
        selected.forEach(item -> item.addClient(created));
        workshops.saveAll(selected);
        return to_response(created);
    }

    /** Actualiza los datos y reemplaza sus asociaciones por la lista de talleres elegida. */
    @Transactional
    public client_response update(String id, client_request request) {
        client existing = find_active(id);
        validate_phones(request);
        validate_duplicate(request, id);
        List<workshop> selected = resolve_workshops(request.workshop_ids());
        existing.getWorkshops().forEach(item -> item.removeClient(existing));
        existing.update(request);
        selected.forEach(item -> item.addClient(existing));
        workshops.saveAll(selected);
        return to_response(clients.save(existing));
    }

    /** Desactiva un cliente conservando sus datos históricos y eliminándolo del listado activo. */
    @Transactional
    public void deactivate(String id) {
        client existing = find_active(id);
        existing.deactivate();
        clients.save(existing);
    }

    /** Valida correo y nombre-teléfono contra todas las filas para impedir clientes duplicados. */
    private void validate_duplicate(client_request request, String current_id) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        boolean duplicate_email = current_id == null ? clients.existsByEmailIgnoreCase(email)
                : clients.existsByEmailIgnoreCaseAndIdNot(email, current_id);
        if (duplicate_email) throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un cliente con ese correo.");
        String phone = normalize_phone(request.personal_phone());
        boolean duplicate_person = current_id == null
                ? clients.existsByIdentity(request.name().trim(), request.paternal_surname().trim(), request.maternal_surname().trim(), phone)
                : clients.existsByIdentityAndIdNot(request.name().trim(), request.paternal_surname().trim(), request.maternal_surname().trim(), phone, current_id);
        if (duplicate_person) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un cliente con ese nombre y teléfono personal.");
        }
    }

    /**
     * Valida en servidor que los cuatro teléfonos normalizados contengan de diez a quince dígitos.
     * Se repite la regla del formulario para impedir que clientes HTTP externos la omitan.
     */
    private void validate_phones(client_request request) {
        List<String> phones = List.of(request.phone(), request.personal_phone(), request.work_phone(), request.alternate_contact_phone());
        boolean invalid = phones.stream().map(this::normalize_phone).anyMatch(value -> !value.matches("^\\+?[0-9]{10,15}$"));
        if (invalid) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cada teléfono debe contener de 10 a 15 dígitos y, opcionalmente, el prefijo +.");
    }

    /** Obtiene todos los talleres solicitados y falla si alguno no existe en el sistema. */
    private List<workshop> resolve_workshops(List<String> ids) {
        Set<String> unique_ids = ids.stream().collect(Collectors.toSet());
        List<workshop> selected = workshops.findAllById(unique_ids);
        if (selected.size() != unique_ids.size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona solo talleres previamente registrados.");
        return selected;
    }

    /** Busca un cliente activo o responde 404 para evitar modificar registros suspendidos. */
    private client find_active(String id) {
        return clients.findByIdAndActiveTrue(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un cliente activo con ese identificador."));
    }

    /** Convierte teléfonos con separadores a su forma canónica antes de comparar duplicados. */
    private String normalize_phone(String value) { return value.replaceAll("[ ()-]", ""); }

    /** Convierte una entidad a DTO sin serializar proxies ni ciclos de la relación muchos a muchos. */
    private client_response to_response(client value) {
        List<client_response.workshop_summary> linked = value.getWorkshops().stream()
                .map(item -> new client_response.workshop_summary(item.getId(), item.getName(), item.getRfc())).toList();
        return new client_response(value.getId(), value.getFecha(), value.getName(), value.getPaternal_surname(),
                value.getMaternal_surname(), value.getEmail(), value.getPhone(), value.getPersonal_phone(),
                value.getWork_phone(), value.getAlternate_contact_phone(), value.getAlternate_contact_name(),
                value.getStreet(), value.getStreet_number(), value.getColonia(), value.getState(),
                value.getMunicipality(), value.getPostal_code(), value.isActive(), linked);
    }
}
