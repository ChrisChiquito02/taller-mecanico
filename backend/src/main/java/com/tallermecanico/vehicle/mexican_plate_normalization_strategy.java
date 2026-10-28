package com.tallermecanico.vehicle;

import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Aplica representación mexicana alfanumérica sin I, Ñ, O ni Q y con separadores eliminados. */
@Component
public class mexican_plate_normalization_strategy implements plate_normalization_strategy {
    /** Normaliza a mayúsculas, elimina espacios y guiones y verifica longitud y caracteres permitidos. */
    @Override
    public String normalize(String raw_plate) {
        String normalized = raw_plate.trim().toUpperCase(Locale.ROOT).replaceAll("[ -]", "");
        if (!normalized.matches("^[A-HJ-NPR-Z0-9]{5,8}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La placa debe contener de 5 a 8 caracteres alfanuméricos válidos; se excluyen I, Ñ, O y Q.");
        }
        return normalized;
    }
}
