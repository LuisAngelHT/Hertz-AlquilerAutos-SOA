package com.hertz.contrato.dto;

import com.hertz.contrato.entity.ContratoDocumento;

import java.time.Instant;

/** Datos del contrato firmado (sin el archivo): lo que se muestra en la tarjeta del contrato. */
public record DocumentoResponse(
        String nombreArchivo,
        String tipoContenido,
        int tamanoBytes,
        String subidoPor,
        Instant subidoEn) {

    public static DocumentoResponse de(ContratoDocumento documento) {
        return new DocumentoResponse(documento.getNombreArchivo(), documento.getTipoContenido(),
                documento.getTamanoBytes(), documento.getSubidoPor(), documento.getSubidoEn());
    }
}
