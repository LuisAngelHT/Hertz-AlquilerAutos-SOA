package com.hertz.flota.controller;

import com.hertz.flota.dto.ActualizarEstadoRequest;
import com.hertz.flota.dto.DisponibilidadResponse;
import com.hertz.flota.dto.VehiculoResponse;
import com.hertz.flota.service.FlotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * API REST de ServicioFlota (SVC-03). Los clientes entran por el API Gateway, que valida el
 * token JWT y envía el rol del usuario en el header X-Rol.
 * El controlador no tiene lógica: delega todo en FlotaService.
 * La documentación (OpenAPI) se ve en /swagger-ui/index.html.
 */
@RestController
@RequestMapping("/api/v1/flota")
@Tag(name = "ServicioFlota (SVC-03)")
public class FlotaController {

    private final FlotaService service;

    public FlotaController(FlotaService service) {
        this.service = service;
    }

    // GET /api/v1/flota/disponibilidad?sucursal=...&categoria=...&fechaInicio=2026-12-01&fechaFin=2026-12-05
    // (fechas en formato ISO). El gateway limita esta ruta a 200 consultas por minuto.
    @GetMapping("/disponibilidad")
    @Operation(summary = "Consulta los vehiculos disponibles por sucursal, categoria y rango de fechas")
    public DisponibilidadResponse disponibilidad(
            @RequestParam String sucursal,
            @RequestParam String categoria,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return service.consultarDisponibilidad(sucursal, categoria, fechaInicio, fechaFin);
    }

    // GET /api/v1/flota/vehiculos/ABC-103 -> datos de un vehículo.
    @GetMapping("/vehiculos/{placa}")
    @Operation(summary = "Consulta un vehiculo por su placa")
    public VehiculoResponse vehiculo(@PathVariable String placa) {
        return service.consultarVehiculo(placa);
    }

    // PUT /api/v1/flota/vehiculos/ABC-103/estado con {"estado":"ALQUILADO"}.
    // El header X-Rol lo pone el gateway; si falta o no es AGENTE/ADMINISTRADOR -> 403.
    @PutMapping("/vehiculos/{placa}/estado")
    @Operation(summary = "Actualiza el estado del vehiculo (solo AGENTE o ADMINISTRADOR)")
    public VehiculoResponse actualizarEstado(
            @PathVariable String placa,
            @RequestHeader(value = "X-Rol", required = false) String rol,
            @Valid @RequestBody ActualizarEstadoRequest request) {
        return service.actualizarEstado(placa, request.estado(), rol);
    }
}
