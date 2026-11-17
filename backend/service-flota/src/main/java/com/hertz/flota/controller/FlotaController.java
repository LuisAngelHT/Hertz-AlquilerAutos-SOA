package com.hertz.flota.controller;

import com.hertz.flota.dto.ActualizarEstadoRequest;
import com.hertz.flota.dto.DisponibilidadResponse;
import com.hertz.flota.dto.HistorialResponse;
import com.hertz.flota.dto.PaginaVehiculosResponse;
import com.hertz.flota.dto.VehiculoResponse;
import com.hertz.flota.entity.EstadoVehiculo;
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
import java.util.List;

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

    // GET /api/v1/flota/vehiculos?texto=toyota&sucursal=LIMA_AEROPUERTO&estado=DISPONIBLE&pagina=0&tamano=12
    // Responde una página: {total, pagina, tamano, totalPaginas, vehiculos}. pagina empieza en 0.
    // Todos los filtros son opcionales; sin "estado" devuelve los DISPONIBLES. Es la búsqueda del
    // cliente (por marca o modelo) y la lista principal del personal.
    @GetMapping("/vehiculos")
    @Operation(summary = "Lista vehiculos por estado, sucursal y texto (marca o modelo)")
    public PaginaVehiculosResponse listar(
            @RequestParam(required = false) EstadoVehiculo estado,
            @RequestParam(required = false) String sucursal,
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamano) {
        return service.listar(estado, sucursal, texto, pagina, tamano);
    }

    // GET /api/v1/flota/vehiculos/ABC-103/historial -> cambios de estado, del más reciente al más antiguo.
    @GetMapping("/vehiculos/{placa}/historial")
    @Operation(summary = "Historial de cambios de estado de un vehiculo")
    public List<HistorialResponse> historial(@PathVariable String placa) {
        return service.consultarHistorial(placa);
    }

    // GET /api/v1/flota/vehiculos/ABC-103 -> datos de un vehículo.
    @GetMapping("/vehiculos/{placa}")
    @Operation(summary = "Consulta un vehiculo por su placa")
    public VehiculoResponse vehiculo(@PathVariable String placa) {
        return service.consultarVehiculo(placa);
    }

    // PUT /api/v1/flota/vehiculos/ABC-103/estado con {"estado":"ALQUILADO"}.
    // El body puede traer un "motivo" opcional. Los headers X-Rol y X-Usuario los pone el gateway;
    // si el rol falta o no puede escribir -> 403. El usuario queda en el historial. X-Sucursal es la
    // sede del usuario: AGENTE y ADMINISTRADOR_SUCURSAL solo modifican vehículos de esa sede (403 si no).
    @PutMapping("/vehiculos/{placa}/estado")
    @Operation(summary = "Actualiza el estado del vehiculo (AGENTE, ADMINISTRADOR o ADMINISTRADOR_SUCURSAL)")
    public VehiculoResponse actualizarEstado(
            @PathVariable String placa,
            @RequestHeader(value = "X-Rol", required = false) String rol,
            @RequestHeader(value = "X-Usuario", required = false) String usuario,
            @RequestHeader(value = "X-Sucursal", required = false) String sucursal,
            @Valid @RequestBody ActualizarEstadoRequest request) {
        return service.actualizarEstado(placa, request.estado(), rol, usuario, sucursal, request.motivo());
    }
}
