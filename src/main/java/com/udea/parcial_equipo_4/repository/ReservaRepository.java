package com.udea.parcial_equipo_4.repository;

import com.udea.parcial_equipo_4.model.Reserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Acceso a datos de reservas: búsqueda por código, listado por espectáculo y suma de sillas reservadas.
 */
public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

    @EntityGraph(attributePaths = {"cliente", "espectaculo"})
    Optional<Reserva> findByCodigoReserva(String codigoReserva);

    @EntityGraph(attributePaths = {"cliente", "espectaculo"})
    Page<Reserva> findByEspectaculoId(Integer espectaculoId, Pageable pageable);

    boolean existsByCodigoReserva(String codigoReserva);

    @Query("SELECT COALESCE(SUM(r.cantidadSillas), 0) FROM Reserva r WHERE r.espectaculo.id = :espectaculoId")
    Integer sumarSillasReservadas(@Param("espectaculoId") Integer espectaculoId);
}
