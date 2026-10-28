package com.tallermecanico.auth;

import jakarta.validation.constraints.*;

public final class AuthDtos {
    /** Evita instanciar la clase contenedora de records de autenticación. */
    private AuthDtos() {}
    /** Datos validados para el registro ordinario de una cuenta. */
    public record RegisterRequest(
            @NotBlank @Size(min=2,max=120) @Pattern(regexp="^[\\p{L}][\\p{L} .'-]*$") String name,
            @NotBlank @Size(min=2,max=120) @Pattern(regexp="^[\\p{L}][\\p{L} .'-]*$") String last_name,
            @NotBlank @Pattern(regexp="^\\+?[0-9 ()-]{10,20}$") String phone,
            @NotBlank @Email @Size(max=190) String email,
            @NotBlank @Size(min=8,max=72) String password,
            @NotBlank @Size(min=8,max=72) String password_confirmation) {}
    /** Credenciales validadas para iniciar sesión. */
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    /** Correo al que se solicita enviar instrucciones de recuperación. */
    public record ForgotRequest(@NotBlank @Email String email) {}
    /** Token temporal y nueva contraseña para completar la recuperación. */
    public record ResetRequest(@NotBlank String token, @NotBlank @Size(min=8,max=72) String password) {}
    /** Datos públicos de la cuenta devueltos por la API. */
    public record UserView(String id, String name, String last_name, String phone, String email, java.time.Instant fecha) {}
    /** Respuesta de autenticación que incluye token y cuenta. */
    public record AuthResponse(String token, UserView user) {}
}
