package com.hertz.reserva.dto;

import java.util.List;

/** Una página del listado de reservas. "pagina" empieza en 0; "total" cuenta todas las que cumplen el filtro. */
public record PaginaReservasResponse(long total, int pagina, int tamano, int totalPaginas,
                                     List<ReservaResponse> reservas) {
}
