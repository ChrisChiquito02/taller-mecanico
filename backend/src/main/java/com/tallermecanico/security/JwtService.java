package com.tallermecanico.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMinutes;
    /** Crea el servicio JWT usando la clave secreta y la vigencia configuradas. */
    public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); this.expirationMinutes = expirationMinutes;
    }
    /** Emite un JWT firmado con identificador, correo y los roles ya asignados a la cuenta. */
    public String issue(String userId, String email, List<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder().subject(userId).claim("email", email).claim("roles", roles)
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(expirationMinutes * 60)))
                .signWith(key).compact();
    }
    /** Valida la firma y devuelve el sujeto del JWT recibido. */
    public String subject(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject(); }
    /** Valida la firma y devuelve los claims autenticados del JWT recibido. */
    public io.jsonwebtoken.Claims claims(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
}
