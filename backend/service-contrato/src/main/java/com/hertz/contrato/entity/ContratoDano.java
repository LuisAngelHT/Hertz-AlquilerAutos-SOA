package com.hertz.contrato.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/** Un dano o defecto anotado en una revision del vehiculo. */
@Entity
@Table(name = "contrato_dano", schema = "contrato")
public class ContratoDano {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ZonaVehiculo zona;

    @Column(nullable = false, length = 300)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GravedadDano gravedad;

    @Column(name = "costo_estimado", precision = 10, scale = 2)
    private BigDecimal costoEstimado;

    /** Ubicacion del dano en el dibujo del vehiculo (null si se anoto sin ubicarlo, por ejemplo un dano interior). */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private VistaVehiculo vista;

    /** Posicion dentro de la vista, de 0 a 100 (porcentaje del ancho y del alto del dibujo). */
    @Column(name = "pos_x", precision = 5, scale = 2)
    private BigDecimal posX;

    @Column(name = "pos_y", precision = 5, scale = 2)
    private BigDecimal posY;

    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    private TipoDano tipo;

    protected ContratoDano() {
    }

    public ContratoDano(ZonaVehiculo zona, String descripcion, GravedadDano gravedad, BigDecimal costoEstimado) {
        this(zona, descripcion, gravedad, costoEstimado, null, null, null, null);
    }

    public ContratoDano(ZonaVehiculo zona, String descripcion, GravedadDano gravedad, BigDecimal costoEstimado,
                        VistaVehiculo vista, BigDecimal posX, BigDecimal posY, TipoDano tipo) {
        this.vista = vista;
        this.posX = posX;
        this.posY = posY;
        this.tipo = tipo;
        this.id = UUID.randomUUID();
        this.zona = zona;
        this.descripcion = descripcion;
        this.gravedad = gravedad;
        this.costoEstimado = costoEstimado;
    }

    public UUID getId() { return id; }
    public ZonaVehiculo getZona() { return zona; }
    public String getDescripcion() { return descripcion; }
    public GravedadDano getGravedad() { return gravedad; }
    public BigDecimal getCostoEstimado() { return costoEstimado; }
    public VistaVehiculo getVista() { return vista; }
    public BigDecimal getPosX() { return posX; }
    public BigDecimal getPosY() { return posY; }
    public TipoDano getTipo() { return tipo; }
}
