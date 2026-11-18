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

    // 401 con aviso de cuántos intentos quedan antes del bloqueo.
    public static AuthException credencialesInvalidas(int intentosRestantes) {
        return new AuthException("CREDENCIALES_INVALIDAS", HttpStatus.UNAUTHORIZED,
                "Correo o contraseña incorrectos. Te quedan " + intentosRestantes
                        + (intentosRestantes == 1 ? " intento" : " intentos") + " antes de bloquear la cuenta");
    }

    // 423: demasiados intentos fallidos; la cuenta queda bloqueada unos minutos.
    public static AuthException cuentaBloqueada(long minutos) {
        return new AuthException("CUENTA_BLOQUEADA", HttpStatus.LOCKED,
                "Demasiados intentos fallidos. Tu cuenta está bloqueada temporalmente; vuelve a intentarlo en "
                        + minutos + (minutos == 1 ? " minuto" : " minutos"));
    }

    public static AuthException usuarioInactivo() {
        return new AuthException("USUARIO_INACTIVO", HttpStatus.FORBIDDEN, "El usuario está inactivo");
    }

    public static AuthException correoYaRegistrado() {
        return new AuthException("CORREO_YA_REGISTRADO", HttpStatus.CONFLICT,
                "Ya existe una cuenta con ese correo");
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