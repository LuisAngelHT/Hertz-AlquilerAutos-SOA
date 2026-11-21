package com.hertz.incidencia.controller;

import com.hertz.incidencia.dto.CierreRequest;
import com.hertz.incidencia.dto.FichaRequest;
import com.hertz.incidencia.dto.FichaResponse;
import com.hertz.incidencia.dto.IncidenciaRequest;
import com.hertz.incidencia.dto.IncidenciaResponse;
import com.hertz.incidencia.dto.PaginaIncidenciasResponse;
import com.hertz.incidencia.dto.RegistroResponse;
import com.hertz.incidencia.dto.SeguimientoRequest;
import com.hertz.incidencia.entity.EstadoIncidencia;
import com.hertz.incidencia.service.FichaTallerService;
import com.hertz.incidencia.service.IncidenciaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidencia")
public class IncidenciaController {

    private final IncidenciaService service;
    private final FichaTallerService fichas;

    public IncidenciaController(IncidenciaService service, FichaTallerService fichas) {
        this.service = service;
        this.fichas = fichas;
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
        return fichas.enriquecer(service.consultar(idVehiculo, estado, pagina, tamano, sucursal));
    }

    @GetMapping("/{id}")
    public IncidenciaResponse obtener(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Sucursal", required = false) String sucursal) {
        return fichas.enriquecer(service.obtener(id, sucursal));
    }

    // PUT /api/v1/incidencia/{id}/cerrar -> la incidencia queda CERRADA y, si era la última abierta del
    // vehículo y este seguía en MANTENIMIENTO, vuelve a DISPONIBLE (mediante flota). Se puede repetir.
    @PutMapping("/{id}/cerrar")
    // Con cuerpo {trabajoRealizado, costoFinal} deja constancia de lo que se reparo (lo usa la ficha del taller);
    // sin cuerpo se comporta como siempre.
    public IncidenciaResponse cerrar(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) CierreRequest cierre,
            @RequestHeader(value = "X-Usuario", required = false, defaultValue = "desconocido") String usuario,
            @RequestHeader(value = "X-Sucursal", required = false) String sucursal) {
        if (cierre != null) {
            fichas.registrarCierre(id, cierre, usuario, sucursal);
        }
        return fichas.enriquecer(service.cerrar(id, sucursal));
    }

    // GET /api/v1/incidencia/{id}/ficha -> ficha tecnica del taller (vacia si todavia no se llenó)
    @GetMapping("/{id}/ficha")
    public FichaResponse obtenerFicha(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Sucursal", required = false) String sucursal) {
        return fichas.obtener(id, sucursal);
    }

    // PUT /api/v1/incidencia/{id}/ficha -> guarda diagnostico, plan, costos y las listas de danos (incidencia abierta)
    @PutMapping("/{id}/ficha")
    public FichaResponse guardarFicha(
            @PathVariable UUID id,
            @Valid @RequestBody FichaRequest ficha,
            @RequestHeader(value = "X-Usuario", required = false, defaultValue = "desconocido") String usuario,
            @RequestHeader(value = "X-Sucursal", required = false) String sucursal) {
        return fichas.guardar(id, ficha, usuario, sucursal);
    }

    // POST /api/v1/incidencia/{id}/ficha/seguimiento -> agrega una nota de avance
    @PostMapping("/{id}/ficha/seguimiento")
    @ResponseStatus(HttpStatus.CREATED)
    public FichaResponse agregarSeguimiento(
            @PathVariable UUID id,
            @Valid @RequestBody SeguimientoRequest seguimiento,
            @RequestHeader(value = "X-Usuario", required = false, defaultValue = "desconocido") String usuario,
            @RequestHeader(value = "X-Sucursal", required = false) String sucursal) {
        return fichas.agregarSeguimiento(id, seguimiento, usuario, sucursal);
    }
}