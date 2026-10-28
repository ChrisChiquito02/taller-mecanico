package com.tallermecanico.workshop;

/** Define el Factory Method centralizado del agregado de taller. */
public interface workshop_factory {
    /** Construye un taller con la solicitud validada y metadatos de idempotencia. */
    workshop create_workshop(create_request request, String photo_path, String idempotency_key, String request_hash);
}
