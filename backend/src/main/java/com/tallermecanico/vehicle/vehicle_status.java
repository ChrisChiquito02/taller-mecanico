package com.tallermecanico.vehicle;

/** Define los estados persistidos del vehículo y evita valores comerciales arbitrarios. */
public enum vehicle_status {
    /** Indica que el vehículo puede administrarse y asociarse a trabajo futuro. */
    activo,
    /** Conserva el registro y sus fotografías sin mostrarlo entre los vehículos activos. */
    inactivo
}
