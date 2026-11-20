package com.hertz.flota.repository;

import com.hertz.flota.entity.EstadoVehiculo;
import com.hertz.flota.entity.Vehiculo;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VehiculoRepository extends JpaRepository<Vehiculo, String> {

    /**
     * Vehiculos en el estado indicado, de la sucursal y categoria pedidas, que no tienen
     * ningun bloqueo (reserva, contrato o mantenimiento) cruzado con el rango [inicio, fin].
     * Un vehiculo ALQUILADO tambien cuenta: su alquiler actual tiene un bloqueo hasta la fecha de
     * devolucion, asi que queda libre para reservas que empiecen despues (el cruce de fechas lo
     * decide). Si no, un auto en la calle desapareceria de todas las reservas futuras.
     */
    @Query("""
            select v from Vehiculo v
            where v.sucursal = :sucursal
              and v.categoria = :categoria
              and (v.estado = :estado or v.estado = com.hertz.flota.entity.EstadoVehiculo.ALQUILADO)
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

    /**
     * Listado general: vehiculos en el estado pedido, de la sucursal que coincida con el patron
     * (use "%" para todas) y cuya marca o modelo contenga el texto (patron en minusculas, "%" = todo).
     * Devuelve una pagina, para no traer miles de vehiculos de una sola vez. Los de la sucursal
     * "prioritaria" (la del usuario; "" si no hay) salen primero y luego las demas por sucursal.
     */
    @Query("""
            select v from Vehiculo v
            where v.estado = :estado
              and v.sucursal like :sucursal
              and lower(concat(v.marca, ' ', v.modelo)) like :texto
            order by case when v.sucursal = :prioritaria then 0 else 1 end,
                     v.sucursal, v.marca, v.modelo, v.placa
            """)
    Page<Vehiculo> listar(@Param("estado") EstadoVehiculo estado,
                          @Param("sucursal") String sucursal,
                          @Param("texto") String texto,
                          @Param("prioritaria") String prioritaria,
                          Pageable pageable);

    /**
     * Igual que buscarDisponibles, pero BLOQUEA las filas devueltas (SELECT ... FOR UPDATE SKIP LOCKED).
     * Si dos reservas piden a la vez la misma categoria, cada una recibe un vehiculo distinto y
     * ninguna espera a la otra: asi no hay sobreventa y escala con muchas reservas concurrentes.
     * Se usa con un Pageable de tamano 1 para tomar solo un vehiculo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("""
            select v from Vehiculo v
            where v.sucursal = :sucursal
              and v.categoria = :categoria
              and (v.estado = :estado or v.estado = com.hertz.flota.entity.EstadoVehiculo.ALQUILADO)
              and not exists (
                  select b.id from BloqueoVehiculo b
                  where b.placa = v.placa
                    and b.fechaInicio <= :fin
                    and b.fechaFin >= :inicio)
            order by v.placa
            """)
    List<Vehiculo> tomarDisponibles(@Param("sucursal") String sucursal,
                                    @Param("categoria") String categoria,
                                    @Param("estado") EstadoVehiculo estado,
                                    @Param("inicio") LocalDate inicio,
                                    @Param("fin") LocalDate fin,
                                    Pageable pageable);

    /**
     * Igual que tomarDisponibles pero para UNA placa concreta: la que eligió el cliente en el catalogo.
     * Devuelve vacio si ese vehiculo ya no esta libre (otro cliente lo reservo, o cambio de estado).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("""
            select v from Vehiculo v
            where v.placa = :placa
              and v.sucursal = :sucursal
              and v.categoria = :categoria
              and (v.estado = :estado or v.estado = com.hertz.flota.entity.EstadoVehiculo.ALQUILADO)
              and not exists (
                  select b.id from BloqueoVehiculo b
                  where b.placa = v.placa
                    and b.fechaInicio <= :fin
                    and b.fechaFin >= :inicio)
            """)
    List<Vehiculo> tomarDisponiblePorPlaca(@Param("placa") String placa,
                                           @Param("sucursal") String sucursal,
                                           @Param("categoria") String categoria,
                                           @Param("estado") EstadoVehiculo estado,
                                           @Param("inicio") LocalDate inicio,
                                           @Param("fin") LocalDate fin);
}
