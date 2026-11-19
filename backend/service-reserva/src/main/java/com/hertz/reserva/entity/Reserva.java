package com.hertz.reserva.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Una reserva de vehículo por categoría y fechas. Se guarda en reserva.reserva.
 * El id lo genera el servicio antes de guardar: es la "referencia" con la que se aparta el
 * vehículo en ServicioFlota. "version" es el control de concurrencia optimista.
 */
@Entity
@Table(name = "reserva", schema = "reserva")
public class Reserva {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "id_cliente", nullable = false)
    private UUID idCliente;

    @Column(name = "categoria", nullable = false, length = 20)
    private String categoria;

    @Column(name = "sucursal_recojo", nullable = false, length = 40)
    private String sucursalRecojo;

    @Column(name = "sucursal_devolucion", nullable = false, length = 40)
    private String sucursalDevolucion;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoReserva estado;

    @Column(name = "placa_asignada", length = 10)
    private String placaAsignada;

    @Column(name = "creada_por", nullable = false, length = 120)
    private String creadaPor;

    // Foto de los datos del cliente al reservar: así el personal ve quién alquiló y cómo contactarlo
    // sin consultar a ServicioCliente por cada reserva de una lista.
    @Column(name = "cliente_nombre", length = 200)
    private String clienteNombre;

    @Column(name = "cliente_documento", length = 30)
    private String clienteDocumento;

    @Column(name = "cliente_telefono", length = 20)
    private String clienteTelefono;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected Reserva() {
    }

    public Reserva(UUID id, UUID idCliente, String categoria, String sucursalRecojo, String sucursalDevolucion,
                   LocalDate fechaInicio, LocalDate fechaFin, String placaAsignada, String creadaPor,
                   String clienteNombre, String clienteDocumento, String clienteTelefono) {
        this.id = id;
        this.idCliente = idCliente;
        this.categoria = categoria;
        this.sucursalRecojo = sucursalRecojo;
        this.sucursalDevolucion = sucursalDevolucion;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.placaAsignada = placaAsignada;
        this.creadaPor = creadaPor;
        this.clienteNombre = clienteNombre;
        this.clienteDocumento = clienteDocumento;
        this.clienteTelefono = clienteTelefono;
        this.estado = EstadoReserva.CONFIRMADA;
        this.creadoEn = Instant.now();
        this.actualizadoEn = this.creadoEn;
    }

    /** Cambia categoría y/o fechas (solo tiene sentido mientras está CONFIRMADA). */
    public void reprogramar(String categoria, LocalDate fechaInicio, LocalDate fechaFin, String placaAsignada) {
        this.categoria = categoria;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.placaAsignada = placaAsignada;
        this.actualizadoEn = Instant.now();
    }

    public void cambiarEstado(EstadoReserva nuevo) {
        this.estado = nuevo;
        this.actualizadoEn = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getIdCliente() { return idCliente; }
    public String getCategoria() { return categoria; }
    public String getSucursalRecojo() { return sucursalRecojo; }
    public String getSucursalDevolucion() { return sucursalDevolucion; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public EstadoReserva getEstado() { return estado; }
    public String getPlacaAsignada() { return placaAsignada; }
    public String getCreadaPor() { return creadaPor; }
    public String getClienteNombre() { return clienteNombre; }
    public String getClienteDocumento() { return clienteDocumento; }
    public String getClienteTelefono() { return clienteTelefono; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }
}
