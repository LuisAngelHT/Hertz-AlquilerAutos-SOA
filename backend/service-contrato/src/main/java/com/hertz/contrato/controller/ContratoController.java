package com.hertz.contrato.controller;

import com.hertz.contrato.dto.ContratoResponse;
import com.hertz.contrato.dto.CrearContratoRequest;
import com.hertz.contrato.dto.PaginaContratosResponse;
import com.hertz.contrato.entity.EstadoContrato;
import com.hertz.contrato.service.ContratoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contrato")
public class ContratoController {

    private final ContratoService service;

    public ContratoController(ContratoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContratoResponse crear(
            @Valid @RequestBody CrearContratoRequest request,
            @RequestHeader(value = "X-Usuario", required = false) String usuario,
            @RequestHeader(value = "X-Rol", required = false) String rol,
            @RequestHeader(value = "X-Sucursal", required = false) String sucursal) {
        return service.crear(request.idReserva(), rol, sucursal, usuario);
    }

    @GetMapping
    public PaginaContratosResponse listar(
            @RequestParam(required = false) UUID idReserva,
            @RequestParam(required = false) UUID idCliente,
            @RequestParam(required = false) String placa,
            @RequestParam(required = false) EstadoContrato estado,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano,
            @RequestHeader(value = "X-Rol", required = false) String rol,
            @RequestHeader(value = "X-Sucursal", required = false) String sucursal) {
        return service.listar(idReserva, idCliente, placa, estado, pagina, tamano, rol, sucursal);
    }

    @GetMapping("/{id}")
    public ContratoResponse obtener(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Rol", required = false) String rol,
            @RequestHeader(value = "X-Sucursal", required = false) String sucursal) {
        return service.obtener(id, rol, sucursal);
    }

    @GetMapping("/reserva/{idReserva}")
    public ContratoResponse obtenerPorReserva(
            @PathVariable UUID idReserva,
            @RequestHeader(value = "X-Rol", required = false) String rol,
            @RequestHeader(value = "X-Sucursal", required = false) String sucursal) {
        return service.obtenerPorReserva(idReserva, rol, sucursal);
    }

    @PutMapping("/reserva/{idReserva}/finalizar")
    public ContratoResponse finalizarPorReserva(
            @PathVariable UUID idReserva,
            @RequestHeader(value = "X-Rol", required = false) String rol,
            @RequestHeader(value = "X-Sucursal", required = false) String sucursal) {
        return service.finalizarPorReserva(idReserva, rol, sucursal);
    }
}
