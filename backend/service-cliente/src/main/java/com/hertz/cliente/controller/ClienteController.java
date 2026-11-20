package com.hertz.cliente.controller;

import com.hertz.cliente.dto.ActualizarClienteRequest;
import com.hertz.cliente.dto.ActualizarPerfilRequest;
import com.hertz.cliente.dto.ClienteRequest;
import com.hertz.cliente.dto.PaginaClientesResponse;
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

    // buscarCliente: AGENTE y ADMINISTRADOR
    @GetMapping
    public ClienteResponse buscar(@RequestParam TipoDocumento tipoDocumento,
                                  @RequestParam String numeroDocumento,
                                  @RequestHeader("X-Rol") String rol) {
        return service.buscar(tipoDocumento, numeroDocumento, rol);
    }

    // Listado de todos los clientes (personal), paginado: ?texto=ana&pagina=0&tamano=10
    @GetMapping("/todos")
    public PaginaClientesResponse listar(@RequestParam(required = false) String texto,
                                         @RequestParam(required = false) Integer pagina,
                                         @RequestParam(required = false) Integer tamano,
                                         @RequestHeader("X-Rol") String rol) {
        return service.listar(texto, pagina, tamano, rol);
    }

    // El perfil del cliente que inició sesión y la edición de su teléfono y licencia (rol CLIENTE).
    @GetMapping("/yo")
    public ClienteResponse miPerfil(@RequestHeader("X-Usuario") String correo, @RequestHeader("X-Rol") String rol) {
        return service.perfilPropio(correo, rol);
    }

    @PutMapping("/yo")
    public ClienteResponse actualizarMiPerfil(@Valid @RequestBody ActualizarPerfilRequest request,
                                              @RequestHeader("X-Usuario") String correo,
                                              @RequestHeader("X-Rol") String rol) {
        return service.actualizarPerfilPropio(correo, rol, request);
    }

    // Uso interno de ServicioReserva (rol SERVICIO): el perfil del cliente logueado, por su correo.
    @GetMapping("/correo")
    public ClienteResponse porCorreo(@RequestParam String email, @RequestHeader("X-Rol") String rol) {
        return service.buscarPorCorreo(email, rol);
    }

    @GetMapping("/{id}")
    public ClienteResponse obtener(@PathVariable UUID id, @RequestHeader("X-Rol") String rol) {
        return service.obtener(id, rol);
    }

    // Registro presencial: AGENTE y ADMINISTRADOR
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponse registrarPresencial(@Valid @RequestBody ClienteRequest request,
                                               @RequestHeader("X-Rol") String rol) {
        return service.registrarPresencial(request, rol);
    }

    // Autoregistro desde el login: público (el gateway no exige token en esta ruta)
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponse registro(@Valid @RequestBody ClienteRequest request) {
        return service.registrar(request);
    }

    // actualizarCliente: AGENTE y ADMINISTRADOR
    @PutMapping("/{id}")
    public ClienteResponse actualizar(@PathVariable UUID id,
                                      @Valid @RequestBody ActualizarClienteRequest request,
                                      @RequestHeader("X-Rol") String rol) {
        return service.actualizar(id, request, rol);
    }
}