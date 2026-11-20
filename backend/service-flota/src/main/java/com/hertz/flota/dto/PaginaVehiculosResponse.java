package com.hertz.flota.dto;

import java.util.List;

/**
 * Una página del listado de vehículos. "total" es la cantidad de vehículos que cumplen el
 * filtro (no solo los de esta página); "pagina" empieza en 0.
 */
public record PaginaVehiculosResponse(long total, int pagina, int tamano, int totalPaginas,
                                      List<VehiculoResponse> vehiculos) {
}
