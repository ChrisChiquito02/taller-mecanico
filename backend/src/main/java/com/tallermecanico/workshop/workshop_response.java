package com.tallermecanico.workshop;

import java.time.Instant;

/** Salida pública del caso de alta, sin clave de idempotencia ni datos internos. */
public record workshop_response(String id, String name, String rfc, String contact_email, String photo_path, String commercial_status, String street, String street_number, String colonia, String postal_code, String state, String municipality, Instant fecha) {
    /** Proyecta los campos expuestos de una entidad persistida. */
    public static workshop_response from(workshop item){return new workshop_response(item.getId(),item.getName(),item.getRfc(),item.getContact_email(),item.getPhoto_path(),item.getCommercial_status().name(),item.getStreet(),item.getStreet_number(),item.getColonia(),item.getPostal_code(),item.getState(),item.getMunicipality(),item.getFecha());}
}
