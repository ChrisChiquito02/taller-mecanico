package com.tallermecanico.workshop;

import org.springframework.stereotype.Component;

/** Implementación concreta del Factory Method para crear talleres con UUIDv7. */
@Component
public class uuidv7_workshop_factory implements workshop_factory {
    /** Delega la construcción a la entidad, que genera UUIDv7 y fecha. */
    @Override public workshop create_workshop(create_request request, String photo_path, String idempotency_key, String request_hash){return new workshop(request,photo_path,idempotency_key,request_hash);}
}
