package com.tallermecanico.auth;

import com.tallermecanico.security.JwtService;
import com.tallermecanico.user.User;
import com.tallermecanico.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class AuthService {
    private final UserRepository users; private final PasswordResetTokenRepository resetTokens;
    private final PasswordEncoder encoder; private final JwtService jwt;
    private final long resetMinutes; private final String frontendUrl; private final SecureRandom random = new SecureRandom();
    /** Construye los casos de uso con repositorios, codificador, tokens y ajustes de recuperación. */
    public AuthService(UserRepository users, PasswordResetTokenRepository resetTokens, PasswordEncoder encoder, JwtService jwt,
                       @Value("${app.reset-token-minutes}") long resetMinutes,
                       @Value("${app.frontend-url}") String frontendUrl) {
        this.users=users; this.resetTokens=resetTokens; this.encoder=encoder; this.jwt=jwt; this.resetMinutes=resetMinutes; this.frontendUrl=frontendUrl;
    }
    /** Indica si el correo está disponible para crear una cuenta, ignorando mayúsculas. */
    @Transactional(readOnly=true) public boolean email_available(String email){return !users.existsByEmailIgnoreCase(email.trim());}
    /** Crea una cuenta ordinaria si el correo no está registrado y devuelve su sesión. */
    @Transactional public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        if (!request.password().equals(request.password_confirmation())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La confirmación de contraseña no coincide.");
        String email=request.email().trim().toLowerCase();
        if(users.existsByEmailIgnoreCase(email)) throw new ResponseStatusException(HttpStatus.CONFLICT,"El correo ya está registrado");
        String phone = request.phone().replaceAll("[ ()-]", "");
        if (!phone.matches("^\\+?[0-9]{10,15}$")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El teléfono debe contener entre 10 y 15 dígitos.");
        return authenticate(new User(request.name().trim(), request.last_name().trim(), phone, email, encoder.encode(request.password())));
    }
    /** Verifica las credenciales y devuelve una sesión JWT válida. */
    @Transactional(readOnly=true) public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        User user=users.findByEmailIgnoreCase(request.email().trim()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Correo o contraseña incorrectos"));
        if(!encoder.matches(request.password(),user.getPasswordHash())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Correo o contraseña incorrectos");
        return response(user);
    }
    /** Persiste una cuenta nueva y produce su respuesta de autenticación. */
    private AuthDtos.AuthResponse authenticate(User user){return response(users.save(user));}
    /** Construye la respuesta pública con JWT y datos de usuario no sensibles. */
    private AuthDtos.AuthResponse response(User user){return new AuthDtos.AuthResponse(jwt.issue(user.getId(),user.getEmail(),user.getRoles().stream().filter(role->role.isActive()).map(role->role.getName()).toList()), new AuthDtos.UserView(user.getId(),user.getName(),user.getLastName(),user.getPhone(),user.getEmail(),user.getFecha()));}
    /** Crea un token de recuperación temporal y lo registra para el correo indicado. */
    @Transactional public void requestPasswordReset(String email) {
        users.findByEmailIgnoreCase(email.trim()).ifPresent(user->{
            resetTokens.deleteByUser_Id(user.getId()); byte[] tokenBytes=new byte[32]; random.nextBytes(tokenBytes);
            String token=Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
            resetTokens.save(new PasswordResetToken(user, sha256(token), Instant.now().plusSeconds(resetMinutes*60)));
            // En producción, reemplazar el log por un proveedor de correo transaccional.
            System.out.println("[RECUPERACIÓN] Enlace temporal para " + user.getEmail() + ": " + frontendUrl + "/reset-password?token=" + token);
        });
    }
    /** Valida el token de recuperación y guarda la contraseña nueva codificada. */
    @Transactional public void resetPassword(String token, String password) {
        PasswordResetToken reset=resetTokens.findByTokenHash(sha256(token)).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enlace inválido o expirado"));
        if(reset.getUsedAt()!=null || reset.getExpiresAt().isBefore(Instant.now())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enlace inválido o expirado");
        reset.getUser().setPasswordHash(encoder.encode(password)); reset.markUsed();
    }
    /** Calcula el resumen SHA-256 hexadecimal usado para guardar tokens sin almacenarlos en claro. */
    private String sha256(String value) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch(Exception e){throw new IllegalStateException(e);}
    }
}
