package com.hertz.cliente.dto;

import java.util.List;

/** Una página del listado de clientes. "pagina" empieza en 0; "total" cuenta todos los que cumplen el filtro. */
public record PaginaClientesResponse(long total, int pagina, int tamano, int totalPaginas,
                                     List<ClienteResponse> clientes) {
}
