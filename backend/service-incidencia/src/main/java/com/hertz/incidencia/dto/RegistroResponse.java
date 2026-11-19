package com.hertz.incidencia.dto;

import com.hertz.incidencia.entity.EstadoIncidencia;

import java.util.UUID;

public record RegistroResponse(UUID idIncidencia, EstadoIncidencia estado, String estadoVehiculo, String idVehiculo) {
}