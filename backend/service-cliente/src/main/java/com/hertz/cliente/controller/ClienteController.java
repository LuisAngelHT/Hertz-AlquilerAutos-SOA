package com.hertz.cliente.controller;

import com.hertz.cliente.dto.ActualizarClienteRequest;
import com.hertz.cliente.dto.ClienteRequest;
import com.hertz.cliente.dto.ClienteResponse;
import com.hertz.cliente.entity.TipoDocumento;
import com.hertz.cliente.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cliente")
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    // buscarCliente
    @GetMapping
    public ClienteResponse buscar(@RequestParam TipoDocumento tipoDocumento,
                                  @RequestParam String numeroDocumento) {
        return service.buscar(tipoDocumento, numeroDocumento);
    }

    @GetMapping("/{id}")
    public ClienteResponse obtener(@PathVariable UUID id) {
        return service.obtener(id);
    }

    // registrarCliente
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponse registrar(@Valid @RequestBody ClienteRequest request) {
        return service.registrar(request);
    }

    // actualizarCliente
    @PutMapping("/{id}")
    public ClienteResponse actualizar(@PathVariable UUID id,
                                      @Valid @RequestBody ActualizarClienteRequest request) {
        return service.actualizar(id, request);
    }
}