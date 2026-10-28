package com.tallermecanico.vehicle;

import java.util.List;
import org.springframework.stereotype.Component;

/** Implementa el Factory Method de vehículo y conserva su vínculo obligatorio con cliente activo. */
@Component
public class uuid_v7_vehicle_factory extends vehicle_factory {
    /** Crea el agregado con UUIDv7, fecha derivada, cliente y fotos ya verificadas. */
    @Override
    public vehicle create_vehicle(vehicle_request request, com.tallermecanico.workshop.client owner,
                                  String normalized_vin, String normalized_plate, List<vehicle_photo> photos) {
        return new vehicle(request, owner, normalized_vin, normalized_plate, photos);
    }
}
