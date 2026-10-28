package com.tallermecanico.role;

import org.springframework.stereotype.Component;

/** Implementación concreta del Factory Method para roles con UUIDv7. */
@Component
public class UuidV7RoleFactory extends RoleFactory {
    /** Construye el objeto Rol nuevo para persistirlo. */
    @Override
    public Role createRole(String name) { return new Role(name); }
}
