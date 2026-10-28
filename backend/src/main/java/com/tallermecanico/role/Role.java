package com.tallermecanico.role;

import com.tallermecanico.security.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/** Representa un rol creado en el catálogo, sin relación ni asignación a cuentas de usuario. */
@Entity
@Table(name = "roles", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class Role {
    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, updatable = false)
    private Instant fecha;

    /** Estado lógico del rol, conservado aunque se suspenda. */
    @Column(nullable = true)
    private Boolean active = true;

    /** Constructor JPA requerido para materializar filas persistidas. */
    protected Role() {}

    /** Crea un rol nuevo con identidad UUIDv7 y la fecha tomada de su marca temporal. */
    public Role(String name) {
        UUID uuid = UuidV7.generate();
        this.id = uuid.toString();
        this.fecha = Instant.ofEpochMilli((uuid.getMostSignificantBits() >>> 16) & 0xFFFFFFFFFFFFL);
        this.name = name;
        this.active = true;
    }

    /** Devuelve el identificador UUIDv7 del rol. */
    public String getId() { return id; }

    /** Devuelve el nombre del rol. */
    public String getName() { return name; }

    /** Devuelve la fecha UTC codificada por el timestamp del UUIDv7. */
    public Instant getFecha() { return fecha; }

    /** Indica si el rol está disponible; las filas previas sin estado se consideran activas. */
    public boolean isActive() { return active == null || active; }

    /** Cambia el estado lógico del rol sin eliminar su fila. */
    public void setActive(boolean active) { this.active = active; }

    /** Actualiza el nombre normalizado del rol. */
    public void setName(String name) { this.name = name; }
}
