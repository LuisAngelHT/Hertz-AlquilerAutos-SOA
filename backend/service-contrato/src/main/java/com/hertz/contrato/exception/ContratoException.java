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

    public static ContratoException contratoNoFirmado() {
        return new ContratoException("CONTRATO_NO_FIRMADO", HttpStatus.BAD_REQUEST,
                "Confirma que el contrato impreso fue firmado por el cliente antes de entregar el vehículo");
    }

    public static ContratoException revisionInvalida(String detalle) {
        return new ContratoException("REVISION_INVALIDA", HttpStatus.BAD_REQUEST, detalle);
    }

    public static ContratoException documentoNoEncontrado() {
        return new ContratoException("DOCUMENTO_NO_ENCONTRADO", HttpStatus.NOT_FOUND,
                "Este contrato todavía no tiene el contrato firmado escaneado");
    }

    public static ContratoException documentoInvalido(String detalle) {
        return new ContratoException("DOCUMENTO_INVALIDO", HttpStatus.BAD_REQUEST, detalle);
    }

    public static ContratoException documentoMuyGrande() {
        return new ContratoException("DOCUMENTO_MUY_GRANDE", HttpStatus.PAYLOAD_TOO_LARGE,
                "El archivo supera el máximo permitido de 5 MB");
    }
}
