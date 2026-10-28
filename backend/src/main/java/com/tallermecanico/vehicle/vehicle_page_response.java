package com.tallermecanico.vehicle;

import java.util.List;

/** Conserva una estructura estable de página para que React controle navegación y total. */
public record vehicle_page_response(List<vehicle_response> content,int number,int size,long totalElements,int totalPages) {}
