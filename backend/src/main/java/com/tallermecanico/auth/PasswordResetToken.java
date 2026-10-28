package com.tallermecanico.auth;

import com.tallermecanico.security.UuidV7;
import com.tallermecanico.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="password_reset_tokens", indexes=@Index(columnList="token_hash", unique=true))
public class PasswordResetToken {
    @Id @Column(length=36) private String id;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="user_id", nullable=false) private User user;
    @Column(name="token_hash", nullable=false, length=64) private String tokenHash;
    @Column(nullable=false) private Instant expiresAt;
    private Instant usedAt;
    @Column(nullable=false, updatable=false) private Instant fecha;
    /** Constructor vacío requerido por JPA. */
    protected PasswordResetToken() {}
    /** Crea un token temporal con identificador UUIDv7 y fecha de expiración. */
    public PasswordResetToken(User user, String tokenHash, Instant expiresAt) {
        UUID uuid=UuidV7.generate(); id=uuid.toString(); fecha=Instant.ofEpochMilli((uuid.getMostSignificantBits()>>>16)&0xFFFFFFFFFFFFL);
        this.user=user; this.tokenHash=tokenHash; this.expiresAt=expiresAt;
    }
    /** Devuelve la cuenta propietaria del token. */
    public User getUser(){return user;}
    /** Devuelve el resumen almacenado del token secreto. */
    public String getTokenHash(){return tokenHash;}
    /** Devuelve el instante de expiración del token. */
    public Instant getExpiresAt(){return expiresAt;}
    /** Devuelve cuándo se consumió el token o nulo si sigue vigente. */
    public Instant getUsedAt(){return usedAt;}
    /** Marca como usado el token para impedir una segunda utilización. */
    public void markUsed(){usedAt=Instant.now();}
}
