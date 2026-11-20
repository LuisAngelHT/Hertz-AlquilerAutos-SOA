package com.hertz.incidencia.service;

import com.hertz.incidencia.client.FlotaClient;
import com.hertz.incidencia.client.VehiculoInfo;
import com.hertz.incidencia.dto.CierreIncidenciaResponse;
import com.hertz.incidencia.dto.IncidenciaRequest;
import com.hertz.incidencia.dto.IncidenciaResponse;
import com.hertz.incidencia.dto.PaginaIncidenciasResponse;
import com.hertz.incidencia.dto.RegistroResponse;
import com.hertz.incidencia.entity.EstadoIncidencia;
import com.hertz.incidencia.entity.Incidencia;
import com.hertz.incidencia.exception.IncidenciaException;
import com.hertz.incidencia.repository.IncidenciaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class IncidenciaService {

    private static final String MANTENIMIENTO = "MANTENIMIENTO";

    private final IncidenciaRepository repository;
    private final FlotaClient flotaClient;

    public IncidenciaService(IncidenciaRepository repository, FlotaClient flotaClient) {
        this.repository = repository;
        this.flotaClient = flotaClient;
    }

    /**
     * registrarIncidencia. No es @Transactional a propósito: no se retiene una conexión de base
     * de datos mientras se espera a ServicioFlota. Primero se aparta el vehículo (MANTENIMIENTO)
     * y luego se guarda la incidencia; si el guardado fallara, el vehículo queda fuera de
     * circulación (lado seguro) y al reintentar se guarda sin volver a cambiarlo.
     */
    public RegistroResponse registrar(IncidenciaRequest request, String usuario, String sucursalUsuario) {
        String placa = normalizarPlaca(request.idVehiculo());

        // Pre-condición 1: el vehículo existe en ServicioFlota
        VehiculoInfo vehiculo = flotaClient.obtenerVehiculo(placa);

        if (restringido(sucursalUsuario) && !vehiculo.sucursal().equalsIgnoreCase(sucursalUsuario.trim())) {
            throw IncidenciaException.sucursalNoAutorizada();
        }

        UUID id = UUID.randomUUID();

        // Post-condición: el vehículo pasa a MANTENIMIENTO mediante SVC-03 (si ya lo está, no se repite)
        if (!MANTENIMIENTO.equals(vehiculo.estado())) {
            flotaClient.pasarAMantenimiento(placa, "Incidencia " + request.tipo() + " (" + id + ")");
        }

        Incidencia guardada = repository.save(new Incidencia(
                id, placa, vehiculo.sucursal(), request.idContrato(), request.tipo(),
                request.descripcion().trim(), request.costoEstimado(), vehiculo.estado(), usuario));

        return new RegistroResponse(guardada.getId(), guardada.getEstado(), MANTENIMIENTO, guardada.getPlaca());
    }

    // consultarIncidencias(idVehiculo): paginado, y el personal de sucursal solo ve las de su sede
    @Transactional(readOnly = true)
    public PaginaIncidenciasResponse consultar(String idVehiculo, EstadoIncidencia estado,
                                               int pagina, int tamano, String sucursalUsuario) {
        int paginaSegura = Math.max(pagina, 0);
        int tamanoSeguro = Math.min(Math.max(tamano, 1), 50);

        Specification<Incidencia> filtro = (raiz, consulta, cb) -> cb.conjunction();
        if (idVehiculo != null && !idVehiculo.isBlank()) {
            String placa = normalizarPlaca(idVehiculo);
            filtro = filtro.and((raiz, consulta, cb) -> cb.equal(raiz.get("placa"), placa));
        }
        if (estado != null) {
            filtro = filtro.and((raiz, consulta, cb) -> cb.equal(raiz.get("estado"), estado));
        }
        if (restringido(sucursalUsuario)) {
            String sucursal = sucursalUsuario.trim().toUpperCase();
            filtro = filtro.and((raiz, consulta, cb) -> cb.equal(raiz.get("sucursal"), sucursal));
        }

        Page<Incidencia> resultado = repository.findAll(filtro,
                PageRequest.of(paginaSegura, tamanoSeguro, Sort.by(Sort.Direction.DESC, "creadoEn")));

        return new PaginaIncidenciasResponse(resultado.getTotalElements(), paginaSegura, tamanoSeguro,
                resultado.getTotalPages(), resultado.getContent().stream().map(this::aRespuesta).toList());
    }

    @Transactional(readOnly = true)
    public IncidenciaResponse obtener(UUID id, String sucursalUsuario) {
        Incidencia incidencia = repository.findById(id).orElseThrow(IncidenciaException::incidenciaNoEncontrada);
        if (restringido(sucursalUsuario) && !incidencia.getSucursal().equalsIgnoreCase(sucursalUsuario.trim())) {
            throw IncidenciaException.incidenciaNoEncontrada();   // fuera de su sede: como si no existiera
        }
        return aRespuesta(incidencia);
    }

    public CierreIncidenciaResponse cerrar(UUID id, String sucursalUsuario) {
        Incidencia incidencia = repository.findById(id)
                .orElseThrow(IncidenciaException::incidenciaNoEncontrada);
        if (restringido(sucursalUsuario)
                && !incidencia.getSucursal().equalsIgnoreCase(sucursalUsuario.trim())) {
            throw IncidenciaException.incidenciaNoEncontrada();
        }

        if (incidencia.getEstado() != EstadoIncidencia.CERRADA) {
            incidencia.cerrar();
            repository.save(incidencia);
        }

        boolean quedanAbiertas = repository.existsByPlacaAndEstadoAndIdNot(
                incidencia.getPlaca(), EstadoIncidencia.ABIERTA, id);
        VehiculoInfo vehiculo = flotaClient.obtenerVehiculo(incidencia.getPlaca());
        String estadoVehiculo = vehiculo.estado();

        if (quedanAbiertas && !MANTENIMIENTO.equals(estadoVehiculo)) {
            flotaClient.pasarAMantenimiento(incidencia.getPlaca(),
                    "Hay otra incidencia abierta para " + incidencia.getPlaca());
            estadoVehiculo = MANTENIMIENTO;
        } else if (!quedanAbiertas && MANTENIMIENTO.equals(estadoVehiculo)) {
            flotaClient.pasarADisponible(incidencia.getPlaca(),
                    "Incidencia " + incidencia.getId() + " cerrada");
            estadoVehiculo = "DISPONIBLE";
        }

        return new CierreIncidenciaResponse(
                incidencia.getId(), incidencia.getEstado(), estadoVehiculo, incidencia.getPlaca());
    }

    private boolean restringido(String sucursalUsuario) {
        return sucursalUsuario != null && !sucursalUsuario.isBlank();
    }

    private String normalizarPlaca(String placa) {
        return placa.trim().toUpperCase();
    }

    private IncidenciaResponse aRespuesta(Incidencia i) {
        return new IncidenciaResponse(i.getId(), i.getPlaca(), i.getSucursal(), i.getIdContrato(),
                i.getTipo(), i.getDescripcion(), i.getCostoEstimado(), i.getEstado(),
                i.getRegistradaPor(), i.getCreadoEn());
    }
}