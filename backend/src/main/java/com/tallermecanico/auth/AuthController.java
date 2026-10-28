package com.tallermecanico.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

@Validated
@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    /** Inicializa el controlador con los casos de uso de autenticación. */
    public AuthController(AuthService auth){this.auth=auth;}
    /** Comprueba anticipadamente si una dirección puede usarse en el formulario de registro. */
    @GetMapping("/register/email-availability") public Map<String,Boolean> email_available(@RequestParam @NotBlank @Email String email){return Map.of("available",auth.email_available(email));}
    /** Registra una cuenta ordinaria con datos validados. */
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) public AuthDtos.AuthResponse register(@Valid @RequestBody AuthDtos.RegisterRequest request){return auth.register(request);}
    /** Autentica correo y contraseña y devuelve el JWT. */
    @PostMapping("/login") public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest request){return auth.login(request);}
    /** Solicita el envío de instrucciones sin revelar si el correo existe. */
    @PostMapping("/forgot-password") public Map<String,String> forgot(@Valid @RequestBody AuthDtos.ForgotRequest request){auth.requestPasswordReset(request.email()); return Map.of("message","Si el correo está registrado, recibirás instrucciones para recuperar tu contraseña.");}
    /** Consume el token de recuperación y actualiza la contraseña. */
    @PostMapping("/reset-password") public Map<String,String> reset(@Valid @RequestBody AuthDtos.ResetRequest request){auth.resetPassword(request.token(),request.password()); return Map.of("message","Contraseña actualizada correctamente.");}
}
