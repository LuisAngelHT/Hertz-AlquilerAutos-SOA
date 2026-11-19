package com.hertz.incidencia.exception;

import org.springframework.http.HttpStatus;

public class IncidenciaException extends RuntimeException {

    private final String codigo;
    private final HttpStatus status;

    public IncidenciaException(String codigo, HttpStatus status, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
        this.status = status;
    }

    public String getCodigo() { return codigo; }
    public HttpStatus getStatus() { return status; }

    public static IncidenciaException vehiculoNoEncontrado() {
        return new IncidenciaException("VEHICULO_NO_ENCONTRADO", HttpStatus.NOT_FOUND,
                "El vehículo no existe en la flota");
    }

    public static IncidenciaException incidenciaNoEncontrada() {
        return new IncidenciaException("INCIDENCIA_NO_ENCONTRADA", HttpStatus.NOT_FOUND,
                "La incidencia no existe");
    }

    public static IncidenciaException sucursalNoAutorizada() {
        return new IncidenciaException("SUCURSAL_NO_AUTORIZADA", HttpStatus.FORBIDDEN,
                "Solo puedes registrar o consultar incidencias de vehículos de tu sucursal");
    }

    public static IncidenciaException transicionInvalida() {
        return new IncidenciaException("TRANSICION_INVALIDA", HttpStatus.CONFLICT,
                "El vehículo no puede pasar a MANTENIMIENTO desde su estado actual");
    }

    public static IncidenciaException servicioNoDisponible(String servicio) {
        return new IncidenciaException("SERVICIO_NO_DISPONIBLE", HttpStatus.SERVICE_UNAVAILABLE,
                servicio + " no está disponible. Reintenta en unos segundos");
    }
}