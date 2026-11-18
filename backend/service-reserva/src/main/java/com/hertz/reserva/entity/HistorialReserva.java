package com.hertz.reserva.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Registro de auditoría: un cambio en una reserva, con quién lo hizo y cuándo.
 * Solo se insertan filas: no tiene setters y el rol de base de datos del servicio no puede
 * actualizar ni borrar esta tabla.
 */
@Entity
@Table(name = "historial_reserva", schema = "reserva")
public class HistorialReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "id_reserva", nullable = false)
    private UUID idReserva;

    /** CREADA, MODIFICADA, EN_CURSO, FINALIZADA o CANCELADA. */
    @Column(name = "accion", nullable = false, length = 20)
    private String accion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20)
    private EstadoReserva estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private EstadoReserva estadoNuevo;

    @Column(name = "usuario", nullable = false, length = 120)
    private String usuario;

    @Column(name = "rol", nullable = false, length = 30)
    private String rol;

    @Column(name = "detalle", length = 300)
    private String detalle;

    @Column(name = "fecha", nullable = false)
    private Instant fecha;

    protected HistorialReserva() {
    }

    public HistorialReserva(UUID idReserva, String accion, EstadoReserva estadoAnterior, EstadoReserva estadoNuevo,
                            String usuario, String rol, String detalle) {
        this.idReserva = idReserva;
        this.accion = accion;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.usuario = usuario;
        this.rol = rol;
        this.detalle = detalle != null && detalle.length() > 300 ? detalle.substring(0, 300) : detalle;
        this.fecha = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getIdReserva() { return idReserva; }
    public String getAccion() { return accion; }
    public EstadoReserva getEstadoAnterior() { return estadoAnterior; }
    public EstadoReserva getEstadoNuevo() { return estadoNuevo; }
    public String getUsuario() { return usuario; }
    public String getRol() { return rol; }
    public String getDetalle() { return detalle; }
    public Instant getFecha() { return fecha; }
}
