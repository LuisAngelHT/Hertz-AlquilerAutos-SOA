package com.hertz.flota.dto;

import java.time.Instant;

/** Formato de error común de todos los servicios: { codigo, mensaje, timestamp }. */
public record ErrorResponse(String codigo, String mensaje, Instant timestamp) {

    public static ErrorResponse of(String codigo, String mensaje) {
        return new ErrorResponse(codigo, mensaje, Instant.now());
    }
}
