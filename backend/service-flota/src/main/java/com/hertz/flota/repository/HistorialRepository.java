package com.hertz.flota.repository;

import com.hertz.flota.entity.HistorialVehiculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HistorialRepository extends JpaRepository<HistorialVehiculo, UUID> {

    /** Los cambios de un vehículo, del más reciente al más antiguo. */
    List<HistorialVehiculo> findByPlacaOrderByFechaDesc(String placa);
}
