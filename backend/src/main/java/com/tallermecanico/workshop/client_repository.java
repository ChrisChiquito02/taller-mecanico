package com.tallermecanico.workshop;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

/** Provee consultas paginadas y criterios de duplicidad de clientes en MySQL. */
public interface client_repository extends JpaRepository<client, String> {
    /** Busca un correo existente sin distinguir mayúsculas y minúsculas. */
    boolean existsByEmailIgnoreCase(String email);
    /** Busca un correo duplicado y excluye el cliente que se está actualizando. */
    boolean existsByEmailIgnoreCaseAndIdNot(String email, String id);
    /** Comprueba duplicados por nombre completo y teléfono personal normalizado. */
    @Query("select case when count(c) > 0 then true else false end from client c where lower(c.name)=lower(:name) and lower(c.paternal_surname)=lower(:paternal) and lower(c.maternal_surname)=lower(:maternal) and c.personal_phone=:phone")
    boolean existsByIdentity(@Param("name") String name, @Param("paternal") String paternal_surname,
                              @Param("maternal") String maternal_surname, @Param("phone") String personal_phone);
    /** Busca duplicidad personal sin contar la propia fila durante una actualización. */
    @Query("select case when count(c) > 0 then true else false end from client c where lower(c.name)=lower(:name) and lower(c.paternal_surname)=lower(:paternal) and lower(c.maternal_surname)=lower(:maternal) and c.personal_phone=:phone and c.id<>:id")
    boolean existsByIdentityAndIdNot(@Param("name") String name, @Param("paternal") String paternal_surname,
                                     @Param("maternal") String maternal_surname, @Param("phone") String personal_phone,
                                     @Param("id") String id);
    /** Obtiene solo clientes activos en páginas, con búsqueda opcional por persona o correo. */
    @Query("select c from client c where c.active = true and (:term = '' or lower(concat(c.name, ' ', c.paternal_surname, ' ', c.maternal_surname)) like lower(concat('%', :term, '%')) or lower(c.email) like lower(concat('%', :term, '%')))")
    Page<client> searchActive(@Param("term") String term, Pageable pageable);
    /** Recupera un cliente activo para editar o suspender sin exponer clientes desactivados. */
    Optional<client> findByIdAndActiveTrue(String id);
    /** Bloquea y devuelve un cliente activo durante el alta para serializar cambios concurrentes de estado. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from client c where c.id=:id and c.active=true")
    Optional<client> findActiveForVehicleRegistration(@Param("id") String id);
}
