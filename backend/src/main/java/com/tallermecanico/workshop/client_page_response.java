package com.tallermecanico.workshop;

import java.util.List;

/** Fija una respuesta estable con metadatos de paginación para las pantallas React. */
public record client_page_response(List<client_response> content, int number, int size,
                                   long totalElements, int totalPages) {}
