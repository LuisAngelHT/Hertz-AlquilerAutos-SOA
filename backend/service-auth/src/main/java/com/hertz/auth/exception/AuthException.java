package com.hertz.auth.exception;

import org.springframework.http.HttpStatus;

public class AuthException extends RuntimeException {

    private final String codigo;
    private final HttpStatus status;

    public AuthException(String codigo, HttpStatus status, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
        this.status = status;
    }

    public String getCodigo() { return codigo; }
    public HttpStatus getStatus() { return status; }

    public static AuthException credencialesInvalidas() {
        return new AuthException("CREDENCIALES_INVALIDAS", HttpStatus.UNAUTHORIZED,
                "Correo o contraseña incorrectos");
    }

    public static AuthException usuarioInactivo() {
        return new AuthException("USUARIO_INACTIVO", HttpStatus.FORBIDDEN, "El usuario está inactivo");
    }

    public static AuthException tokenInvalido() {
        return new AuthException("TOKEN_INVALIDO", HttpStatus.UNAUTHORIZED, "El token no es válido");
    }

    public static AuthException tokenExpirado() {
        return new AuthException("TOKEN_EXPIRADO", HttpStatus.UNAUTHORIZED, "El token ha expirado");
    }

    public static AuthException tokenAusente() {
        return new AuthException("TOKEN_AUSENTE", HttpStatus.UNAUTHORIZED,
                "Falta el header Authorization: Bearer <token>");
    }
}