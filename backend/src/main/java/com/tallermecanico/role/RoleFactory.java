package com.tallermecanico.role;

/** Define el método de fábrica que crea instancias de rol. */
public abstract class RoleFactory {
    /** Punto de extensión Factory Method para construir un rol validado. */
    public abstract Role createRole(String name);
}
