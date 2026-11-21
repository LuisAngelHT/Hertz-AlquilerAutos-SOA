package com.hertz.incidencia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/** Dano interno o mecanico encontrado por el mecanico (motor, frenos, transmision...). */
@Entity
@Table(name = "ficha_dano_interno", schema = "incidencia")
public class FichaDanoInterno {

    @Id
    private UUID id;

    @Column(name = "id_incidencia", nullable = false)
    private UUID idIncidencia;

    @Column(nullable = false)
    private int orden;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SistemaVehiculo sistema;

    @Column(nullable = false, length = 300)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GravedadDano gravedad;

    @Column(name = "costo_estimado", precision = 10, scale = 2)
    private BigDecimal costoEstimado;

    protected FichaDanoInterno() {
    }

    public FichaDanoInterno(UUID idIncidencia, int orden, SistemaVehiculo sistema, String descripcion,
                            GravedadDano gravedad, BigDecimal costoEstimado) {
        this.id = UUID.randomUUID();
        this.idIncidencia = idIncidencia;
        this.orden = orden;
        this.sistema = sistema;
        this.descripcion = descripcion;
        this.gravedad = gravedad;
        this.costoEstimado = costoEstimado;
    }

    public UUID getId() { return id; }
    public UUID getIdIncidencia() { return idIncidencia; }
    public int getOrden() { return orden; }
    public SistemaVehiculo getSistema() { return sistema; }
    public String getDescripcion() { return descripcion; }
    public GravedadDano getGravedad() { return gravedad; }
    public BigDecimal getCostoEstimado() { return costoEstimado; }
}
