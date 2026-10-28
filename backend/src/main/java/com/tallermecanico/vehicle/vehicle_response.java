package com.tallermecanico.vehicle;

import java.time.Instant;
import java.util.List;

/** Devuelve la ficha de vehículo sin serializar entidades JPA ni ciclos de asociaciones. */
public record vehicle_response(String id,Instant fecha,String vin,String plate,String make,String model,int year,
                               String version,String color,Long mileage,String status,String mechanic_id,
                               client_summary client,List<photo_summary> photos) {
    /** Resume al cliente para identificar al propietario asociado en pantalla. */
    public record client_summary(String id,String name,String paternal_surname,String maternal_surname,String email) {}
    /** Expone metadatos de imagen y su endpoint seguro de consulta autenticada. */
    public record photo_summary(String id,String file_name,String content_type,long size_bytes,String url) {}
}
