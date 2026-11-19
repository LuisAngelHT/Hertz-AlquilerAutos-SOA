package com.hertz.contrato.dto;

import java.util.List;

public record PaginaContratosResponse(
        long total,
        int pagina,
        int tamano,
        int totalPaginas,
        List<ContratoResponse> contratos) {
}
