package com.hertz.incidencia.controller;

import com.hertz.incidencia.dto.IncidenciaRequest;
import com.hertz.incidencia.dto.IncidenciaResponse;
import com.hertz.incidencia.dto.PaginaIncidenciasResponse;
import com.hertz.incidencia.dto.RegistroResponse;
import com.hertz.incidencia.entity.EstadoIncidencia;
import com.hertz.incidencia.service.IncidenciaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidencia")
public class IncidenciaController {

    private final IncidenciaService service;

    public IncidenciaController(IncidenciaService service) {
        this.service = service;
    }

    // registrarIncidencia
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegistroResponse registrar(
            @Valid @RequestBody IncidenciaRequest request,
            @RequestHeader(value = "X-Usuario", required = false, defaultValue = "desconocido") String usuario,
            @RequestHeader(value = "X-Sucursal", required = false) String sucursal) {
        return service.registrar(request, usuario, sucursal);
    }

    // consultarIncidencias(idVehiculo)
    @GetMapping
    public PaginaIncidenciasResponse consultar(
            @RequestParam(required = false) String idVehiculo,
            @RequestParam(required = false) EstadoIncidencia estado,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano,
            @RequestHeader(value = "X-Sucursal", required = false) String sucursal) {
        return service.consultar(idVehiculo, estado, pagina, tamano, sucursal);
    }

    @GetMapping("/{id}")
    public IncidenciaResponse obtener(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Sucursal", required = false) String sucursal) {
        return service.obtener(id, sucursal);
    }

    // PUT /api/v1/incidencia/{id}/cerrar -> la incidencia queda CERRADA y, si era la última abierta del
    // vehículo y este seguía en MANTENIMIENTO, vuelve a DISPONIBLE (mediante flota). Se puede repetir.
    @PutMapping("/{id}/cerrar")
    public IncidenciaResponse cerrar(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Sucursal", required = false) String sucursal) {
        return service.cerrar(id, sucursal);
    }
}