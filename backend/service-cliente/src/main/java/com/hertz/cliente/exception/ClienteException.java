package com.hertz.cliente.exception;

import org.springframework.http.HttpStatus;

public class ClienteException extends RuntimeException {

    private final String codigo;
    private final HttpStatus status;

    public ClienteException(String codigo, HttpStatus status, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
        this.status = status;
    }

    public String getCodigo() { return codigo; }
    public HttpStatus getStatus() { return status; }

    public static ClienteException clienteNoEncontrado() {
        return new ClienteException("CLIENTE_NO_ENCONTRADO", HttpStatus.NOT_FOUND,
                "El cliente no existe");
    }

    public static ClienteException documentoInvalido(String detalle) {
        return new ClienteException("DOCUMENTO_INVALIDO", HttpStatus.BAD_REQUEST, detalle);
    }

    public static ClienteException clienteDuplicado() {
        return new ClienteException("CLIENTE_DUPLICADO", HttpStatus.CONFLICT,
                "Ya existe un cliente con ese documento");
    }
    public static ClienteException rolNoPermitido() {
        return new ClienteException("ROL_NO_PERMITIDO", HttpStatus.FORBIDDEN,
                "Solo AGENTE o ADMINISTRADOR pueden consultar o modificar datos de clientes");
    }
    public static ClienteException correoYaRegistrado() {
        return new ClienteException("CORREO_YA_REGISTRADO", HttpStatus.CONFLICT,
                "Ya existe una cuenta con ese correo");
    }

    public static ClienteException servicioAuthNoDisponible() {
        return new ClienteException("SERVICIO_AUTH_NO_DISPONIBLE", HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo crear la cuenta: el servicio de autenticación no está disponible");
    }
}