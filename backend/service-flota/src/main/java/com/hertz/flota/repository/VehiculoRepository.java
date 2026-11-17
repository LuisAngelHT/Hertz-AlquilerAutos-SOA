package com.hertz.flota.repository;

import com.hertz.flota.entity.EstadoVehiculo;
import com.hertz.flota.entity.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VehiculoRepository extends JpaRepository<Vehiculo, String> {

    /**
     * Vehiculos en el estado indicado, de la sucursal y categoria pedidas, que no tienen
     * ningun bloqueo (reserva, contrato o mantenimiento) cruzado con el rango [inicio, fin].
     */
    @Query("""
            select v from Vehiculo v
            where v.sucursal = :sucursal
              and v.categoria = :categoria
              and v.estado = :estado
              and not exists (
                  select b.id from BloqueoVehiculo b
                  where b.placa = v.placa
                    and b.fechaInicio <= :fin
                    and b.fechaFin >= :inicio)
            order by v.placa
            """)
    List<Vehiculo> buscarDisponibles(@Param("sucursal") String sucursal,
                                     @Param("categoria") String categoria,
                                     @Param("estado") EstadoVehiculo estado,
                                     @Param("inicio") LocalDate inicio,
                                     @Param("fin") LocalDate fin);
}
