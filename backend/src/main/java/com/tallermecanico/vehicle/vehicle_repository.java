package com.tallermecanico.vehicle;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Proporciona consulta paginada y restricciones de unicidad para VIN y placa normalizados. */
public interface vehicle_repository extends JpaRepository<vehicle,String> {
    /** Detecta cualquier VIN ya guardado, incluso si su vehículo fue desactivado. */
    boolean existsByVin(String vin);
    /** Detecta cualquier placa normalizada ya guardada, incluso si su vehículo fue desactivado. */
    boolean existsByPlate(String plate);
    /** Busca vehículos activos por identificadores, marca, modelo o nombre del cliente. */
    @Query("select v from vehicle v join v.owner c where v.status=:status and (:term='' or lower(v.vin) like lower(concat('%',:term,'%')) or lower(v.plate) like lower(concat('%',:term,'%')) or lower(v.make) like lower(concat('%',:term,'%')) or lower(v.model) like lower(concat('%',:term,'%')) or lower(concat(c.name,' ',c.paternal_surname,' ',c.maternal_surname)) like lower(concat('%',:term,'%'))) ")
    Page<vehicle> search_active(@Param("term") String term,@Param("status") vehicle_status status,Pageable pageable);
    /** Recupera la ficha completa activa para detalle o para entregar un archivo asociado. */
    Optional<vehicle> findByIdAndStatus(String id,vehicle_status status);
}
