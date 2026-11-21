package com.hertz.reserva.controller;

import com.hertz.reserva.dto.CancelarRequest;
import com.hertz.reserva.dto.HistorialReservaResponse;
import com.hertz.reserva.dto.ModificarReservaRequest;
import com.hertz.reserva.dto.PaginaReservasResponse;
import com.hertz.reserva.dto.ReservaRequest;
import com.hertz.reserva.dto.ReservaResponse;
import com.hertz.reserva.entity.EstadoReserva;
import com.hertz.reserva.service.ReservaService;
import com.hertz.reserva.service.ReservaService.Actor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import java.util.List;
import java.util.UUID;

/**
 * API REST de ServicioReserva (SVC-02). Los clientes entran por el API Gateway, que valida el
 * token JWT y envía X-Usuario (correo), X-Rol y X-Sucursal. El controlador no tiene lógica:
 * delega todo en ReservaService. Documentación OpenAPI en /swagger-ui/index.html.
 */
@RestController
@RequestMapping("/api/v1/reserva")
@Tag(name = "ServicioReserva (SVC-02)")
public class ReservaController {

    private final ReservaService service;

    public ReservaController(ReservaService service) {
        this.service = service;
    }

    // POST /api/v1/reserva  -> crearReserva. 201 con la reserva CONFIRMADA.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una reserva (valida al cliente y aparta un vehiculo en ServicioFlota)")
    public ReservaResponse crear(@Valid @RequestBody ReservaRequest request,
                                 @RequestHeader(value = "X-Usuario", required = false) String usuario,
                                 @RequestHeader(value = "X-Rol", required = false) String rol,
                                 @RequestHeader(value = "X-Sucursal", required = false) String sucursal) {
        return service.crear(request, new Actor(usuario, rol, sucursal));
    }

    // GET /api/v1/reserva?tipoDocumento=DNI&numeroDocumento=12345678&estado=CONFIRMADA&sucursal=LIMA_AEROPUERTO&pagina=0&tamano=12
    // consultarReserva por documento o con filtros. Responde una página {total, pagina, tamano, totalPaginas, reservas}.
    // El CLIENTE solo recibe las suyas; el personal de sucursal, las que salen o llegan a su sede.
    // recogenHoy=true: reservas confirmadas que empiezan hoy (aviso de qué clientes llegan).
    @GetMapping
    @Operation(summary = "Lista reservas (por documento del cliente, estado o sucursal), paginado")
    public PaginaReservasResponse listar(
            @RequestParam(required = false) String tipoDocumento,
            @RequestParam(required = false) String numeroDocumento,
            @RequestParam(required = false) UUID idCliente,
            @RequestParam(required = false) EstadoReserva estado,
            @RequestParam(required = false) String sucursal,
            @RequestParam(required = false) Boolean recogenHoy,
            @RequestParam(required = false) String placa,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamano,
            @RequestHeader(value = "X-Usuario", required = false) String usuario,
            @RequestHeader(value = "X-Rol", required = false) String rol,
            @RequestHeader(value = "X-Sucursal", required = false) String headerSucursal) {
        return service.listar(tipoDocumento, numeroDocumento, idCliente, estado, sucursal, recogenHoy, placa, pagina,
                tamano, new Actor(usuario, rol, headerSucursal));
    }

    // GET /api/v1/reserva/{idReserva}  -> consultarReserva por id.
    @GetMapping("/{id}")
    @Operation(summary = "Consulta una reserva por su id")
    public ReservaResponse obtener(@PathVariable UUID id,
                                   @RequestHeader(value = "X-Usuario", required = false) String usuario,
                                   @RequestHeader(value = "X-Rol", required = false) String rol,
                                   @RequestHeader(value = "X-Sucursal", required = false) String sucursal) {
        return service.obtener(id, new Actor(usuario, rol, sucursal));
    }

    // PUT /api/v1/reserva/{idReserva}  -> modificarReserva: {fechaInicio?, fechaFin?, categoria?} o {estado, motivo?}.
    @PutMapping("/{id}")
    @Operation(summary = "Modifica fechas/categoria de una reserva CONFIRMADA, o cambia su estado")
    public ReservaResponse modificar(@PathVariable UUID id,
                                     @Valid @RequestBody ModificarReservaRequest request,
                                     @RequestHeader(value = "X-Usuario", required = false) String usuario,
                                     @RequestHeader(value = "X-Rol", required = false) String rol,
                                     @RequestHeader(value = "X-Sucursal", required = false) String sucursal) {
        return service.modificar(id, request, new Actor(usuario, rol, sucursal));
    }

    // POST /api/v1/reserva/{idReserva}/cancelar  -> cancelarReserva. Queda CANCELADA y se libera el vehiculo.
    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancela una reserva y libera el vehiculo apartado")
    public ReservaResponse cancelar(@PathVariable UUID id,
                                    @Valid @RequestBody(required = false) CancelarRequest request,
                                    @RequestHeader(value = "X-Usuario", required = false) String usuario,
                                    @RequestHeader(value = "X-Rol", required = false) String rol,
                                    @RequestHeader(value = "X-Sucursal", required = false) String sucursal) {
        return service.cancelar(id, request == null ? null : request.motivo(), new Actor(usuario, rol, sucursal));
    }

    // GET /api/v1/reserva/{idReserva}/historial  -> quien cambio la reserva y cuando (solo personal).
    @GetMapping("/{id}/historial")
    @Operation(summary = "Historial de cambios de una reserva (solo personal)")
    public List<HistorialReservaResponse> historial(@PathVariable UUID id,
                                                    @RequestHeader(value = "X-Usuario", required = false) String usuario,
                                                    @RequestHeader(value = "X-Rol", required = false) String rol,
                                                    @RequestHeader(value = "X-Sucursal", required = false) String sucursal) {
        return service.consultarHistorial(id, new Actor(usuario, rol, sucursal));
    }
}
