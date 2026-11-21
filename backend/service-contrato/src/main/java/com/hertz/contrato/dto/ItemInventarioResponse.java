package com.hertz.contrato.dto;

import com.hertz.contrato.entity.ContratoItem;

import java.math.BigDecimal;

public record ItemInventarioResponse(String item, String estado, BigDecimal costoEstimado) {

    public static ItemInventarioResponse de(ContratoItem item) {
        return new ItemInventarioResponse(item.getItem().name(), item.getEstado().name(), item.getCostoEstimado());
    }
}
