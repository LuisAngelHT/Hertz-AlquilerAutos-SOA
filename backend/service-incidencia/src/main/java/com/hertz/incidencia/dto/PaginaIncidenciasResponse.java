package com.hertz.incidencia.dto;

import java.util.List;

public record PaginaIncidenciasResponse(long total, int pagina, int tamano, int totalPaginas,
                                        List<IncidenciaResponse> incidencias) {
}