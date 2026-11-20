package com.hertz.reserva.exception;

import com.hertz.reserva.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * Convierte cualquier error en una respuesta JSON { codigo, mensaje, timestamp }: el cliente
 * nunca recibe una traza de Java.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ReservaException.class)
    public ResponseEntity<ErrorResponse> handleReserva(ReservaException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ErrorResponse.of(ex.getCodigo(), ex.getMessage()));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleConcurrencia(OptimisticLockingFailureException ex) {
        ReservaException r = ReservaException.conflictoConcurrencia();
        return ResponseEntity.status(r.getStatus()).body(ErrorResponse.of(r.getCodigo(), r.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getDefaultMessage()).collect(Collectors.joining("; "));
        return solicitudInvalida(detalle);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleBody(HttpMessageNotReadableException ex) {
        return solicitudInvalida("El cuerpo de la solicitud no es valido (revisa el JSON, las fechas AAAA-MM-DD y el estado)");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleParam(MissingServletRequestParameterException ex) {
        return solicitudInvalida("Falta el parametro obligatorio: " + ex.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleType(MethodArgumentTypeMismatchException ex) {
        return solicitudInvalida("Valor invalido para el parametro: " + ex.getName());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleHeader(MissingRequestHeaderException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of("ROL_NO_AUTORIZADO", "Falta la identidad del usuario (header " + ex.getHeaderName() + ")"));
    }

    // Último recurso: se registra la causa en el log y el cliente recibe un 500 limpio.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOtro(Exception ex) {
        log.error("Error inesperado en ServicioReserva", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("ERROR_INTERNO", "Ocurrio un error inesperado"));
    }

    private ResponseEntity<ErrorResponse> solicitudInvalida(String detalle) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.of("SOLICITUD_INVALIDA", detalle));
    }
}
