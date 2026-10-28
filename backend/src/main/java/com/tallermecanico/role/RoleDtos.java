package com.tallermecanico.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** DTOs de entrada y salida del caso de uso de creación de roles. */
public final class RoleDtos {
    /** Evita instanciar la clase contenedora de records. */
    private RoleDtos() {}

    /** Entrada para crear un rol con nombre compuesto únicamente por palabras y separadores permitidos. */
    public record CreateRequest(
            @NotBlank @Size(max = 80)
            @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?:[ '-][A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$",
                    message = "El nombre solo admite letras, espacios, apóstrofes y guiones.")
            String name) {}

    /** Salida que confirma los datos del rol creado. */
    public record RoleResponse(String id, String name, Instant fecha, boolean active) {}

    /** Entrada para renombrar un rol existente con la misma sintaxis que su creación. */
    public record UpdateRequest(
            @NotBlank @Size(max = 80)
            @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?:[ '-][A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$",
                    message = "El nombre solo admite letras, espacios, apóstrofes y guiones.")
            String name) {}

    /** Entrada que expresa el estado lógico solicitado para un rol. */
    public record StatusRequest(boolean active) {}

    /** Salida booleana para validar duplicados antes de enviar la creación. */
    public record ExistsResponse(boolean exists) {}
}
