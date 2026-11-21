package com.hertz.contrato.dto;

import com.hertz.contrato.entity.ContratoInspeccion;
import com.hertz.contrato.entity.ContratoItem;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record InspeccionResponse(
        int kilometraje,
        String combustible,
        String observaciones,
        Boolean clienteConforme,
        String registradoPor,
        Instant registradoEn,
        List<DanoResponse> danos,
        List<ItemInventarioResponse> inventario) {

    public static InspeccionResponse de(ContratoInspeccion inspeccion) {
        return new InspeccionResponse(
                inspeccion.getKilometraje(),
                inspeccion.getCombustible().name(),
                inspeccion.getObservaciones(),
                inspeccion.getClienteConforme(),
                inspeccion.getRegistradoPor(),
                inspeccion.getRegistradoEn(),
                // Al traer danos e inventario juntos la base repite cada dano (uno por elemento): se descartan los repetidos.
                inspeccion.getDanos().stream().distinct().map(DanoResponse::de).toList(),
                inspeccion.getItems().stream()
                        .sorted(Comparator.comparingInt((ContratoItem i) -> i.getItem().ordinal()))
                        .map(ItemInventarioResponse::de)
                        .toList());
    }
}
