package com.hertz.flota.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Rango de fechas en el que un vehículo está ocupado (por una reserva, un contrato o un
 * mantenimiento). Se guarda en flota.bloqueo_vehiculo.
 *
 * Se usa solo para leer: la consulta de disponibilidad descarta los vehículos que tengan un
 * bloqueo que se cruce con las fechas pedidas. Por ahora los bloqueos se cargan en la base;
 * más adelante los registrará ServicioReserva.
 */
@Entity
@Table(name = "bloqueo_vehiculo", schema = "flota")
public class BloqueoVehiculo {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "placa", nullable = false, length = 10)
    private String placa;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "motivo", nullable = false, length = 20)
    private String motivo;

    @Column(name = "referencia", length = 50)
    private String referencia;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected BloqueoVehiculo() {
    }

    public UUID getId() { return id; }
    public String getPlaca() { return placa; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public String getMotivo() { return motivo; }
    public String getReferencia() { return referencia; }
}
