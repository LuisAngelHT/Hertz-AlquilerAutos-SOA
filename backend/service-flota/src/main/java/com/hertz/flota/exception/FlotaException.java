package com.hertz.flota.exception;

import org.springframework.http.HttpStatus;

/**
 * Error de negocio del servicio de flota. Lleva el código del error (el que define el
 * documento) y el estado HTTP con el que se responde. GlobalExceptionHandler la convierte
 * en un ErrorResponse JSON.
 */
public class FlotaException extends RuntimeException {

    private final String codigo;
    private final HttpStatus status;

    public FlotaException(String codigo, HttpStatus status, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
        this.status = status;
    }

    public String getCodigo() { return codigo; }
    public HttpStatus getStatus() { return status; }

    // 404: la placa no existe en la flota.
    public static FlotaException vehiculoNoEncontrado(String placa) {
        return new FlotaException("VEHICULO_NO_ENCONTRADO", HttpStatus.NOT_FOUND,
                "No existe un vehiculo con placa " + placa);
    }

    // 409: se quiso alquilar un vehículo que no está DISPONIBLE (ya alquilado o en mantenimiento).
    public static FlotaException vehiculoNoDisponible(String placa) {
        return new FlotaException("VEHICULO_NO_DISPONIBLE", HttpStatus.CONFLICT,
                "El vehiculo " + placa + " no esta en estado DISPONIBLE");
    }

    // 403: el rol del usuario no puede cambiar el estado (solo AGENTE y ADMINISTRADOR).
    public static FlotaException rolNoAutorizado() {
        return new FlotaException("ROL_NO_AUTORIZADO", HttpStatus.FORBIDDEN,
                "Solo los roles AGENTE o ADMINISTRADOR pueden cambiar el estado de un vehiculo");
    }

    // 400: datos mal enviados (por ejemplo fechaFin anterior a fechaInicio).
    public static FlotaException solicitudInvalida(String detalle) {
        return new FlotaException("SOLICITUD_INVALIDA", HttpStatus.BAD_REQUEST, detalle);
    }
}
