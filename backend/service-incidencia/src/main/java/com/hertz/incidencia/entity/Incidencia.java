package com.hertz.incidencia.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// DynamicUpdate: al cerrar una incidencia solo se actualiza la columna "estado", que es la única
// sobre la que svc_incidencia tiene permiso UPDATE (ver db/incidencia.sql). Las demás son de solo escritura inicial.
@org.hibernate.annotations.DynamicUpdate
@Entity
@Table(name = "incidencia", schema = "incidencia")
public class Incidencia {

    @Id
    private UUID id;

    @Column(nullable = false, length = 10)
    private String placa;

    @Column(nullable = false, length = 40)
    private String sucursal;

    @Column(name = "id_contrato")
    private UUID idContrato;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoIncidencia tipo;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(name = "costo_estimado", precision = 10, scale = 2)
    private BigDecimal costoEstimado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoIncidencia estado;

    @Column(name = "estado_vehiculo_previo", length = 20)
    private String estadoVehiculoPrevio;

    @Column(name = "registrada_por", nullable = false, length = 120)
    private String registradaPor;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    protected Incidencia() {
    }

    public Incidencia(UUID id, String placa, String sucursal, UUID idContrato, TipoIncidencia tipo,
                      String descripcion, BigDecimal costoEstimado, String estadoVehiculoPrevio,
                      String registradaPor) {
        this.id = id;
        this.placa = placa;
        this.sucursal = sucursal;
        this.idContrato = idContrato;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.costoEstimado = costoEstimado;
        this.estadoVehiculoPrevio = estadoVehiculoPrevio;
        this.registradaPor = registradaPor;
        this.estado = EstadoIncidencia.ABIERTA;
        this.creadoEn = Instant.now();
    }

    /** Cierra la incidencia. Si ya estaba cerrada no hace nada (repetirlo es seguro). */
    public void cerrar() {
        this.estado = EstadoIncidencia.CERRADA;
    }

    public UUID getId() { return id; }
    public String getPlaca() { return placa; }
    public String getSucursal() { return sucursal; }
    public UUID getIdContrato() { return idContrato; }
    public TipoIncidencia getTipo() { return tipo; }
    public String getDescripcion() { return descripcion; }
    public BigDecimal getCostoEstimado() { return costoEstimado; }
    public EstadoIncidencia getEstado() { return estado; }
    public String getRegistradaPor() { return registradaPor; }
    public Instant getCreadoEn() { return creadoEn; }
}