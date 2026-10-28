package com.tallermecanico.workshop;

import java.time.Instant;
import java.util.List;

/** Presenta los datos del cliente sin exponer entidades JPA ni relaciones recursivas. */
public record client_response(
        String id, Instant fecha, String name, String paternal_surname, String maternal_surname,
        String email, String phone, String personal_phone, String work_phone,
        String alternate_contact_phone, String alternate_contact_name, String street,
        String street_number, String colonia, String state, String municipality,
        String postal_code, boolean active, List<workshop_summary> workshops
) {
    /** Resume un taller vinculado para que el formulario pueda mostrarlo con seguridad. */
    public record workshop_summary(String id, String name, String rfc) {}
}
