package com.tallermecanico.workshop;

/** Campos del catálogo de talleres requeridos para elegir asociaciones de cliente. */
public record workshop_summary(String id, String name, String rfc, String commercial_status) {}
