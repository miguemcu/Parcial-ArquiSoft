package com.udea.parcial_equipo_4.repository;

import com.udea.parcial_equipo_4.model.Espectaculo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Acceso a datos de espectáculos, con lectura bloqueante ({@code PESSIMISTIC_WRITE})
 * para crear reservas sin sobreventa.
 */
public interface EspectaculoRepository extends JpaRepository<Espectaculo, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Espectaculo e WHERE e.id = :id")
    Optional<Espectaculo> findByIdForUpdate(@Param("id") Integer id);
}
