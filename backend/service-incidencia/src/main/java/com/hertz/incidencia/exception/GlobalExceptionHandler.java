package com.hertz.incidencia.exception;

import com.hertz.incidencia.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IncidenciaException.class)
    public ResponseEntity<ErrorResponse> handleIncidencia(IncidenciaException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ErrorResponse.of(ex.getCodigo(), ex.getMessage()));
    }

    // Falta el tipo o la descripción (o algún dato mal formado): INCIDENCIA_INVALIDA según la ficha
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ErrorResponse.of("INCIDENCIA_INVALIDA", detalle));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.of("SOLICITUD_INVALIDA",
                "El cuerpo no es un JSON válido (revisa idContrato: debe ser un UUID, y costoEstimado: un número)"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTipo(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.of("SOLICITUD_INVALIDA",
                "Valor incorrecto en '" + ex.getName() + "'"));
    }
}