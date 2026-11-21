package com.hertz.contrato.dto;

import java.time.Instant;
import java.util.UUID;

/** Fila liviana (sin el archivo) para consultar varios contratos a la vez. */
public record DocumentoMeta(
        UUID idContrato,
        String nombreArchivo,
        String tipoContenido,
        int tamanoBytes,
        String subidoPor,
        Instant subidoEn) {

    public DocumentoResponse respuesta() {
        return new DocumentoResponse(nombreArchivo, tipoContenido, tamanoBytes, subidoPor, subidoEn);
    }
}
