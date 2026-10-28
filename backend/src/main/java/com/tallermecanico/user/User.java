package com.tallermecanico.user;

import com.tallermecanico.security.UuidV7;
import com.tallermecanico.role.Role;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity @Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class User {
    @Id @Column(length = 36, updatable = false, nullable = false) private String id;
    @Column(nullable = false, length = 120) private String name;
    /** Apellido capturado en el registro actualizado; permite conservar cuentas previas sin apellido. */
    @Column(name = "last_name", length = 120) private String lastName;
    /** Teléfono normalizado capturado al registrar una cuenta. */
    @Column(length = 20) private String phone;
    @Column(nullable = false, length = 190) private String email;
    @Column(name = "password_hash", nullable = false, length = 100) private String passwordHash;
    /** Fecha UTC derivada del timestamp de UUIDv7; permite consultas y orden explícitos. */
    @Column(nullable = false, updatable = false) private Instant fecha;
    @Column(nullable = false, updatable = false) private Instant createdAt;

    /** Roles asignados explícitamente a esta cuenta; el registro público los deja vacíos. */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new LinkedHashSet<>();

    /** Constructor vacío requerido por JPA. */
    protected User() {}
    /** Crea una cuenta ordinaria con UUIDv7 y su fecha UTC derivada. */
    public User(String name, String lastName, String phone, String email, String passwordHash) {
        UUID uuid = UuidV7.generate(); this.id = uuid.toString();
        long timestampMillis = (uuid.getMostSignificantBits() >>> 16) & 0xFFFFFFFFFFFFL;
        this.fecha = Instant.ofEpochMilli(timestampMillis); this.createdAt = this.fecha;
        this.name = name; this.lastName = lastName; this.phone = phone; this.email = email; this.passwordHash = passwordHash;
    }
    /** Devuelve el UUIDv7 de la cuenta. */
    public String getId() { return id; }
    /** Devuelve el nombre de la cuenta. */
    public String getName() { return name; }
    /** Devuelve el apellido registrado, que puede ser nulo en cuentas antiguas. */
    public String getLastName() { return lastName; }
    /** Devuelve el teléfono registrado, que puede ser nulo en cuentas antiguas. */
    public String getPhone() { return phone; }
    /** Devuelve el correo de acceso. */
    public String getEmail() { return email; }
    /** Devuelve el hash BCrypt de la contraseña. */
    public String getPasswordHash() { return passwordHash; }
    /** Devuelve la fecha derivada del UUIDv7. */
    public Instant getFecha() { return fecha; }
    /** Devuelve la fecha de creación de la cuenta. */
    public Instant getCreatedAt() { return createdAt; }
    /** Devuelve los roles persistidos de esta cuenta para autorizar su sesión. */
    public Set<Role> getRoles() { return roles; }
    /** Asigna un rol explícito a esta cuenta. */
    public void addRole(Role role) { this.roles.add(role); }
    /** Reemplaza el hash de contraseña durante el flujo de recuperación. */
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
