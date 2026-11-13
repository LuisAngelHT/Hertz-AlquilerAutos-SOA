package com.hertz.cliente.exception;

import com.hertz.cliente.dto.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ClienteException.class)
    public ResponseEntity<ErrorResponse> handleCliente(ClienteException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ErrorResponse.of(ex.getCodigo(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ErrorResponse.of("SOLICITUD_INVALIDA", detalle));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.of("SOLICITUD_INVALIDA",
                "El cuerpo no es un JSON válido o tiene valores incorrectos (revisa tipoDocumento y las fechas, formato AAAA-MM-DD)"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTipo(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.of("DOCUMENTO_INVALIDO",
                "Valor incorrecto en '" + ex.getName() + "' (tipoDocumento: DNI, CE o PASAPORTE; id: UUID)"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleFaltaParametro(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.of("SOLICITUD_INVALIDA",
                "Falta el parámetro '" + ex.getParameterName() + "'"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegridad(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of("CLIENTE_DUPLICADO",
                "El registro entra en conflicto con uno existente"));
    }
}