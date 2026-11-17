package com.hertz.flota.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Registro de auditoría: un cambio de estado de un vehículo, con quién lo hizo y cuándo.
 * Se guarda en flota.historial_vehiculo. Solo se insertan filas: no tiene setters y el rol
 * de base de datos del servicio no puede actualizar ni borrar esta tabla.
 */
@Entity
@Table(name = "historial_vehiculo", schema = "flota")
public class HistorialVehiculo {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "placa", nullable = false, length = 10)
    private String placa;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", nullable = false, length = 20)
    private EstadoVehiculo estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private EstadoVehiculo estadoNuevo;

    @Column(name = "usuario", nullable = false, length = 120)
    private String usuario;

    @Column(name = "rol", nullable = false, length = 30)
    private String rol;

    @Column(name = "motivo", length = 200)
    private String motivo;

    @Column(name = "fecha", nullable = false, updatable = false)
    private Instant fecha;

    /** Constructor vacío que exige JPA. */
    protected HistorialVehiculo() {
    }

    public HistorialVehiculo(String placa, EstadoVehiculo estadoAnterior, EstadoVehiculo estadoNuevo,
                             String usuario, String rol, String motivo) {
        this.id = UUID.randomUUID();
        this.placa = placa;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.usuario = usuario;
        this.rol = rol;
        this.motivo = motivo;
        this.fecha = Instant.now();
    }

    public UUID getId() { return id; }
    public String getPlaca() { return placa; }
    public EstadoVehiculo getEstadoAnterior() { return estadoAnterior; }
    public EstadoVehiculo getEstadoNuevo() { return estadoNuevo; }
    public String getUsuario() { return usuario; }
    public String getRol() { return rol; }
    public String getMotivo() { return motivo; }
    public Instant getFecha() { return fecha; }
}
