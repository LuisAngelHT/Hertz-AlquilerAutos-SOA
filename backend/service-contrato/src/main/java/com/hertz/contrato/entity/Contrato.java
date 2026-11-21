package com.hertz.contrato.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "contrato", schema = "contrato")
public class Contrato {

    @Id
    private UUID id;

    @Column(name = "id_reserva", nullable = false, unique = true)
    private UUID idReserva;

    @Column(name = "id_cliente", nullable = false)
    private UUID idCliente;

    @Column(nullable = false, length = 10)
    private String placa;

    @Column(name = "sucursal_recojo", nullable = false, length = 40)
    private String sucursalRecojo;

    @Column(name = "sucursal_devolucion", nullable = false, length = 40)
    private String sucursalDevolucion;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin_prevista", nullable = false)
    private LocalDate fechaFinPrevista;

    @Column(name = "fecha_devolucion")
    private LocalDate fechaDevolucion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoContrato estado;

    @Column(name = "registrado_por", nullable = false, length = 120)
    private String registradoPor;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    protected Contrato() {
    }

    public Contrato(UUID id, UUID idReserva, UUID idCliente, String placa,
                    String sucursalRecojo, String sucursalDevolucion,
                    LocalDate fechaInicio, LocalDate fechaFinPrevista, String registradoPor) {
        this.id = id;
        this.idReserva = idReserva;
        this.idCliente = idCliente;
        this.placa = placa;
        this.sucursalRecojo = sucursalRecojo;
        this.sucursalDevolucion = sucursalDevolucion;
        this.fechaInicio = fechaInicio;
        this.fechaFinPrevista = fechaFinPrevista;
        this.estado = EstadoContrato.VIGENTE;
        this.registradoPor = registradoPor;
        this.creadoEn = Instant.now();
        this.actualizadoEn = this.creadoEn;
    }

    /**
     * El inicio real de un alquiler es el día en que se entrega el vehículo, no la fecha de la reserva. Si el
     * contrato quedó con un inicio posterior (la reserva era para más adelante y se entregó antes), se corrige.
     */
    public void ajustarInicioA(LocalDate entregaReal) {
        if (fechaInicio.isAfter(entregaReal)) {
            fechaInicio = entregaReal;
        }
    }

    public void finalizar(LocalDate fecha) {
        if (estado == EstadoContrato.FINALIZADO) {
            return;
        }
        fechaDevolucion = fecha;
        estado = EstadoContrato.FINALIZADO;
        actualizadoEn = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getIdReserva() { return idReserva; }
    public UUID getIdCliente() { return idCliente; }
    public String getPlaca() { return placa; }
    public String getSucursalRecojo() { return sucursalRecojo; }
    public String getSucursalDevolucion() { return sucursalDevolucion; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public LocalDate getFechaFinPrevista() { return fechaFinPrevista; }
    public LocalDate getFechaDevolucion() { return fechaDevolucion; }
    public EstadoContrato getEstado() { return estado; }
    public String getRegistradoPor() { return registradoPor; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }
}
