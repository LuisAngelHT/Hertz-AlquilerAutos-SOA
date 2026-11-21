package com.hertz.incidencia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/** Dano de carroceria que el mecanico agrega sobre el dibujo del vehiculo (los de la devolucion vienen del contrato). */
@Entity
@Table(name = "ficha_dano_externo", schema = "incidencia")
public class FichaDanoExterno {

    @Id
    private UUID id;

    @Column(name = "id_incidencia", nullable = false)
    private UUID idIncidencia;

    @Column(nullable = false)
    private int orden;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private VistaVehiculo vista;

    @Column(name = "pos_x", nullable = false, precision = 5, scale = 2)
    private BigDecimal posX;

    @Column(name = "pos_y", nullable = false, precision = 5, scale = 2)
    private BigDecimal posY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private TipoDano tipo;

    @Column(nullable = false, length = 300)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GravedadDano gravedad;

    @Column(name = "costo_estimado", precision = 10, scale = 2)
    private BigDecimal costoEstimado;

    protected FichaDanoExterno() {
    }

    public FichaDanoExterno(UUID idIncidencia, int orden, VistaVehiculo vista, BigDecimal posX, BigDecimal posY,
                            TipoDano tipo, String descripcion, GravedadDano gravedad, BigDecimal costoEstimado) {
        this.id = UUID.randomUUID();
        this.idIncidencia = idIncidencia;
        this.orden = orden;
        this.vista = vista;
        this.posX = posX;
        this.posY = posY;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.gravedad = gravedad;
        this.costoEstimado = costoEstimado;
    }

    public UUID getId() { return id; }
    public UUID getIdIncidencia() { return idIncidencia; }
    public int getOrden() { return orden; }
    public VistaVehiculo getVista() { return vista; }
    public BigDecimal getPosX() { return posX; }
    public BigDecimal getPosY() { return posY; }
    public TipoDano getTipo() { return tipo; }
    public String getDescripcion() { return descripcion; }
    public GravedadDano getGravedad() { return gravedad; }
    public BigDecimal getCostoEstimado() { return costoEstimado; }
}
