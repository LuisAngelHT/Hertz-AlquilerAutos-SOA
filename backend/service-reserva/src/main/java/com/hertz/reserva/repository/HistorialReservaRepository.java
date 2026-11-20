package com.hertz.reserva.repository;

import com.hertz.reserva.entity.HistorialReserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HistorialReservaRepository extends JpaRepository<HistorialReserva, UUID> {

    List<HistorialReserva> findByIdReservaOrderByFechaDesc(UUID idReserva);
}
