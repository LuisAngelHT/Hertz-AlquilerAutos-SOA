package com.hertz.contrato.exception;

import org.springframework.http.HttpStatus;

public class ContratoException extends RuntimeException {

    private final String codigo;
    private final HttpStatus status;

    public ContratoException(String codigo, HttpStatus status, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
        this.status = status;
    }

    public String getCodigo() { return codigo; }
    public HttpStatus getStatus() { return status; }

    public static ContratoException contratoNoEncontrado() {
        return new ContratoException("CONTRATO_NO_ENCONTRADO", HttpStatus.NOT_FOUND,
                "El contrato no existe");
    }

    public static ContratoException reservaNoEncontrada() {
        return new ContratoException("RESERVA_NO_ENCONTRADA", HttpStatus.NOT_FOUND,
                "La reserva asociada no existe");
    }

    public static ContratoException reservaNoDisponible(String detalle) {
        return new ContratoException("RESERVA_NO_DISPONIBLE", HttpStatus.CONFLICT, detalle);
    }

    public static ContratoException rolNoAutorizado() {
        return new ContratoException("ROL_NO_AUTORIZADO", HttpStatus.FORBIDDEN,
                "Tu rol no tiene acceso a los contratos");
    }

    public static ContratoException sucursalNoAutorizada() {
        return new ContratoException("SUCURSAL_NO_AUTORIZADA", HttpStatus.FORBIDDEN,
                "Solo puedes consultar o gestionar contratos de tu sucursal");
    }

    public static ContratoException transicionInvalida(String detalle) {
        return new ContratoException("TRANSICION_INVALIDA", HttpStatus.CONFLICT, detalle);
    }

    public static ContratoException servicioNoDisponible(String servicio, Throwable causa) {
        return new ContratoException("SERVICIO_NO_DISPONIBLE", HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo comunicar con " + servicio + ". Inténtalo nuevamente.");
    }
}
