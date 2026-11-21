package com.hertz.incidencia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Ficha tecnica del taller de una incidencia: diagnostico, plan, costos y, al cerrar, lo que se reparo. */
@Entity
@Table(name = "ficha_taller", schema = "incidencia")
public class FichaTaller {

    @Id
    @Column(name = "id_incidencia")
    private UUID idIncidencia;

    @Column(length = 1000)
    private String diagnostico;

    @Column(name = "dias_estimados")
    private Integer diasEstimados;

    @Column(name = "fecha_salida_prevista")
    private LocalDate fechaSalidaPrevista;

    @Column(name = "costo_repuestos", precision = 10, scale = 2)
    private BigDecimal costoRepuestos;

    @Column(name = "costo_mano_obra", precision = 10, scale = 2)
    private BigDecimal costoManoObra;

    @Column(name = "trabajo_realizado", length = 1000)
    private String trabajoRealizado;

    @Column(name = "costo_final", precision = 10, scale = 2)
    private BigDecimal costoFinal;

    @Column(name = "actualizado_por", nullable = false, length = 120)
    private String actualizadoPor;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    protected FichaTaller() {
    }

    public FichaTaller(UUID idIncidencia, String usuario) {
        this.idIncidencia = idIncidencia;
        this.actualizadoPor = usuario;
        this.actualizadoEn = Instant.now();
    }

    public void actualizarPlan(String diagnostico, Integer diasEstimados, LocalDate fechaSalidaPrevista,
                               BigDecimal costoRepuestos, BigDecimal costoManoObra, String usuario) {
        this.diagnostico = diagnostico;
        this.diasEstimados = diasEstimados;
        this.fechaSalidaPrevista = fechaSalidaPrevista;
        this.costoRepuestos = costoRepuestos;
        this.costoManoObra = costoManoObra;
        tocar(usuario);
    }

    public void registrarCierre(String trabajoRealizado, BigDecimal costoFinal, String usuario) {
        this.trabajoRealizado = trabajoRealizado;
        this.costoFinal = costoFinal;
        tocar(usuario);
    }

    private void tocar(String usuario) {
        this.actualizadoPor = usuario;
        this.actualizadoEn = Instant.now();
    }

    public UUID getIdIncidencia() { return idIncidencia; }
    public String getDiagnostico() { return diagnostico; }
    public Integer getDiasEstimados() { return diasEstimados; }
    public LocalDate getFechaSalidaPrevista() { return fechaSalidaPrevista; }
    public BigDecimal getCostoRepuestos() { return costoRepuestos; }
    public BigDecimal getCostoManoObra() { return costoManoObra; }
    public String getTrabajoRealizado() { return trabajoRealizado; }
    public BigDecimal getCostoFinal() { return costoFinal; }
    public String getActualizadoPor() { return actualizadoPor; }
    public Instant getActualizadoEn() { return actualizadoEn; }
}
