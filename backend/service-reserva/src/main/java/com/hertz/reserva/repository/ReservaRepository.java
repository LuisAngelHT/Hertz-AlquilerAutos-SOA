package com.hertz.reserva.repository;

import com.hertz.reserva.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.UUID;

/**
 * Acceso a reserva.reserva. El listado usa Specification (filtros opcionales) en vez de una
 * consulta con "parametro is null", que PostgreSQL no puede tipar bien para uuid.
 */
public interface ReservaRepository extends JpaRepository<Reserva, UUID>, JpaSpecificationExecutor<Reserva> {

    /** Cuántas reservas de un cliente están en alguno de esos estados (para limitar las reservas activas). */
    long countByIdClienteAndEstadoIn(UUID idCliente, Collection<com.hertz.reserva.entity.EstadoReserva> estados);
}
