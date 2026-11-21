package com.hertz.contrato.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/** Estado de un elemento del inventario en una revision. El costo (opcional) solo se usa en la devolucion. */
@Entity
@Table(name = "contrato_item", schema = "contrato")
public class ContratoItem {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ItemInventario item;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private EstadoItem estado;

    @Column(name = "costo_estimado", precision = 10, scale = 2)
    private BigDecimal costoEstimado;

    protected ContratoItem() {
    }

    public ContratoItem(ItemInventario item, EstadoItem estado, BigDecimal costoEstimado) {
        this.id = UUID.randomUUID();
        this.item = item;
        this.estado = estado;
        this.costoEstimado = costoEstimado;
    }

    public UUID getId() { return id; }
    public ItemInventario getItem() { return item; }
    public EstadoItem getEstado() { return estado; }
    public BigDecimal getCostoEstimado() { return costoEstimado; }
}
