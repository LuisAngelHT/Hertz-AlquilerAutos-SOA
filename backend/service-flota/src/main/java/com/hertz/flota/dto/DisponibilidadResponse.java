package com.hertz.flota.dto;

import java.util.List;

/** Respuesta de la consulta de disponibilidad: la categoría pedida, cuántos hay y la lista de vehículos. */
public record DisponibilidadResponse(String categoria, int cantidadDisponible, List<VehiculoResponse> vehiculos) {
}
