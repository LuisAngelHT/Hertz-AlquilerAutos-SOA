package com.hertz.incidencia.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TipoIncidencia {
    DAÑO,
    MANTENIMIENTO,
    SINIESTRO;

    // Acepta "DAÑO" y "DANO". Un valor desconocido queda null y la validación responde INCIDENCIA_INVALIDA.
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static TipoIncidencia desde(String valor) {
        if (valor == null) {
            return null;
        }
        String v = valor.trim().toUpperCase();
        if (v.equals("DANO")) {
            v = "DAÑO";
        }
        for (TipoIncidencia tipo : values()) {
            if (tipo.name().equals(v)) {
                return tipo;
            }
        }
        return null;
    }
}