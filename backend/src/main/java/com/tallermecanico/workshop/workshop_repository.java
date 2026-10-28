package com.tallermecanico.workshop;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Consultas persistentes necesarias para evitar talleres duplicados e idempotencia. */
public interface workshop_repository extends JpaRepository<workshop,String> {
    /** Devuelve talleres registrados en orden de creación para el selector de asociación. */
    java.util.List<workshop> findAllByOrderByFechaDesc();
    /** Busca un taller mediante su RFC normalizado, que actúa como clave de negocio única. */
    Optional<workshop> findByRfc(String rfc);
    /** Busca el resultado anterior asociado a una clave de idempotencia. */
    @Query("select item from workshop item where item.idempotency_key = :key")
    Optional<workshop> find_idempotency_key(@Param("key") String idempotency_key);
    /** Comprueba si existe un taller con el RFC recibido. */
    boolean existsByRfc(String rfc);
}
