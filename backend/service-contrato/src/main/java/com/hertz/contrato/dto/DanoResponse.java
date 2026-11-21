package com.hertz.contrato.dto;

import com.hertz.contrato.entity.ContratoDano;

import java.math.BigDecimal;

public record DanoResponse(String zona, String descripcion, String gravedad, BigDecimal costoEstimado,
                           String vista, BigDecimal posX, BigDecimal posY, String tipo) {

    public static DanoResponse de(ContratoDano dano) {
        return new DanoResponse(dano.getZona().name(), dano.getDescripcion(), dano.getGravedad().name(),
                dano.getCostoEstimado(),
                dano.getVista() == null ? null : dano.getVista().name(),
                dano.getPosX(), dano.getPosY(),
                dano.getTipo() == null ? null : dano.getTipo().name());
    }
}
