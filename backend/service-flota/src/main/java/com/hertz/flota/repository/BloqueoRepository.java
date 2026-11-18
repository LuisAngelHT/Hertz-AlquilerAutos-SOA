package com.hertz.flota.repository;

import com.hertz.flota.entity.BloqueoVehiculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface BloqueoRepository extends JpaRepository<BloqueoVehiculo, UUID> {

    /** El bloqueo creado para una referencia (por ejemplo, el id de una reserva). */
    Optional<BloqueoVehiculo> findFirstByReferenciaAndMotivo(String referencia, String motivo);

    /** Cuántos bloqueos (reservas, contratos) de una placa siguen vigentes desde "hoy" en adelante. */
    long countByPlacaAndFechaFinGreaterThanEqualAndMotivoIn(String placa, LocalDate hoy, Collection<String> motivos);
}
