package com.hertz.contrato.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Revision del vehiculo. La de ENTREGA deja constancia de como sale (kilometraje, combustible y danos que
 * ya tenia); la de DEVOLUCION, de como vuelve: sus danos son los nuevos y quedan a cargo del cliente.
 */
@Entity
@Table(name = "contrato_inspeccion", schema = "contrato")
public class ContratoInspeccion {

    @Id
    private UUID id;

    @Column(name = "id_contrato", nullable = false)
    private UUID idContrato;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoInspeccion tipo;

    @Column(nullable = false)
    private int kilometraje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Combustible combustible;

    @Column(length = 500)
    private String observaciones;

    /** Solo en la devolucion: el cliente acepto lo registrado. En la entrega es null. */
    @Column(name = "cliente_conforme")
    private Boolean clienteConforme;

    @Column(name = "registrado_por", nullable = false, length = 120)
    private String registradoPor;

    @Column(name = "registrado_en", nullable = false)
    private Instant registradoEn;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_inspeccion", nullable = false, updatable = false)
    private List<ContratoDano> danos = new ArrayList<>();

    /** Inventario de Antes (entrega) o Despues (devolucion). Es un Set para poder traerlo junto con los danos. */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_inspeccion", nullable = false, updatable = false)
    private Set<ContratoItem> items = new LinkedHashSet<>();

    protected ContratoInspeccion() {
    }

    public ContratoInspeccion(UUID idContrato, TipoInspeccion tipo, int kilometraje, Combustible combustible,
                              String observaciones, Boolean clienteConforme, String registradoPor,
                              List<ContratoDano> danos) {
        this(idContrato, tipo, kilometraje, combustible, observaciones, clienteConforme, registradoPor, danos, List.of());
    }

    public ContratoInspeccion(UUID idContrato, TipoInspeccion tipo, int kilometraje, Combustible combustible,
                              String observaciones, Boolean clienteConforme, String registradoPor,
                              List<ContratoDano> danos, List<ContratoItem> items) {
        this.id = UUID.randomUUID();
        this.idContrato = idContrato;
        this.tipo = tipo;
        this.kilometraje = kilometraje;
        this.combustible = combustible;
        this.observaciones = observaciones;
        this.clienteConforme = clienteConforme;
        this.registradoPor = registradoPor;
        this.registradoEn = Instant.now();
        this.danos.addAll(danos);
        this.items.addAll(items);
    }

    public UUID getId() { return id; }
    public UUID getIdContrato() { return idContrato; }
    public TipoInspeccion getTipo() { return tipo; }
    public int getKilometraje() { return kilometraje; }
    public Combustible getCombustible() { return combustible; }
    public String getObservaciones() { return observaciones; }
    public Boolean getClienteConforme() { return clienteConforme; }
    public String getRegistradoPor() { return registradoPor; }
    public Instant getRegistradoEn() { return registradoEn; }
    public List<ContratoDano> getDanos() { return danos; }
    public Set<ContratoItem> getItems() { return items; }
}
