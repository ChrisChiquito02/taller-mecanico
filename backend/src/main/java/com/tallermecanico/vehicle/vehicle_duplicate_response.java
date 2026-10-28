package com.tallermecanico.vehicle;

/** Devuelve detectores separados para que la pantalla marque VIN y placa duplicados. */
public record vehicle_duplicate_response(boolean vin_exists,boolean plate_exists) {}
