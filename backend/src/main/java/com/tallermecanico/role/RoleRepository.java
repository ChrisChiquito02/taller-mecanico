package com.tallermecanico.role;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/** Acceso persistente al catálogo de roles. */
public interface RoleRepository extends JpaRepository<Role, String> {
    /** Busca duplicados por nombre sin distinguir mayúsculas y minúsculas. */
    boolean existsByNameIgnoreCase(String name);
    /** Comprueba duplicados al renombrar, excluyendo el rol que se está editando. */
    boolean existsByNameIgnoreCaseAndIdNot(String name, String id);
    /** Devuelve los roles ordenados por la fecha UUIDv7 más reciente. */
    List<Role> findAllByOrderByFechaDesc();
}
