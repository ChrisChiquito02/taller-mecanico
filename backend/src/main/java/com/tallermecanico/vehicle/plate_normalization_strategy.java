package com.tallermecanico.vehicle;

/** Abstrae la normalización de placas para permitir reglas distintas por jurisdicción. */
public interface plate_normalization_strategy {
    /** Devuelve la placa normalizada o rechaza un formato que no sea compatible. */
    String normalize(String raw_plate);
}
