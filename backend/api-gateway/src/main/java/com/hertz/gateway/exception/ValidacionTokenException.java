package com.hertz.gateway.exception;

import org.springframework.http.HttpStatus;

public class ValidacionTokenException extends RuntimeException {

    private final HttpStatus status;

    public ValidacionTokenException(HttpStatus status, String cuerpoJson) {
        super(cuerpoJson);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
