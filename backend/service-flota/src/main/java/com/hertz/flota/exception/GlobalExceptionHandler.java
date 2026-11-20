package com.hertz.flota.exception;

import com.hertz.flota.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * Convierte cualquier error del servicio en una respuesta JSON con el formato común
 * { codigo, mensaje, timestamp }. Así el cliente nunca recibe una traza de Java.
 * Los errores de validación (parámetro faltante, JSON mal formado, valor de enum inválido)
 * se responden como 400 SOLICITUD_INVALIDA.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FlotaException.class)
    public ResponseEntity<ErrorResponse> handleFlota(FlotaException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ErrorResponse.of(ex.getCodigo(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return solicitudInvalida(detalle);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleBody(HttpMessageNotReadableException ex) {
        return solicitudInvalida("El cuerpo de la solicitud no es valido (revisa el JSON y el valor de estado)");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleParam(MissingServletRequestParameterException ex) {
        return solicitudInvalida("Falta el parametro obligatorio: " + ex.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleType(MethodArgumentTypeMismatchException ex) {
        return solicitudInvalida("Valor invalido para el parametro: " + ex.getName());
    }

    private ResponseEntity<ErrorResponse> solicitudInvalida(String detalle) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("SOLICITUD_INVALIDA", detalle));
    }
}
