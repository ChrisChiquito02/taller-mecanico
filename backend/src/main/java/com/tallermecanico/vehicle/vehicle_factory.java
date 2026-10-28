package com.tallermecanico.vehicle;

/** Declara el Factory Method que centraliza la creación de vehículos del dominio. */
public abstract class vehicle_factory {
    /** Construye un vehículo validado con su cliente activo, datos normalizados y fotografías. */
    public abstract vehicle create_vehicle(vehicle_request request, com.tallermecanico.workshop.client owner,
                                           String normalized_vin, String normalized_plate,
                                           java.util.List<vehicle_photo> photos);
}
