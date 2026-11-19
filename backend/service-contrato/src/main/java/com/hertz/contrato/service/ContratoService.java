package com.hertz.contrato.service;

import com.hertz.contrato.client.ReservaClient;
import com.hertz.contrato.client.ReservaInfo;
import com.hertz.contrato.dto.ContratoResponse;
import com.hertz.contrato.dto.PaginaContratosResponse;
import com.hertz.contrato.entity.Contrato;
import com.hertz.contrato.entity.EstadoContrato;
import com.hertz.contrato.exception.ContratoException;
import com.hertz.contrato.repository.ContratoRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ContratoService {

    private static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";
    private static final String ROL_SERVICIO = "SERVICIO";
    private static final Set<String> ROLES_PERSONAL =
            Set.of("AGENTE", "ADMINISTRADOR", "ADMINISTRADOR_SUCURSAL");
    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");
    private static final int TAMANO_MAXIMO = 50;

    private final ContratoRepository repository;
    private final ReservaClient reservaClient;

    public ContratoService(ContratoRepository repository, ReservaClient reservaClient) {
        this.repository = repository;
        this.reservaClient = reservaClient;
    }

    /**
     * Crea el contrato al entregar el vehículo. ServicioReserva proporciona la reserva y
     * solo se crea cuando esta se encuentra EN_CURSO.
     */
    public ContratoResponse crear(UUID idReserva, String rol, String sucursal, String usuario) {
        exigirAcceso(rol);

        var existente = repository.findByIdReserva(idReserva);
        if (existente.isPresent()) {
            Contrato contrato = existente.get();
            autorizarSucursal(rol, sucursal, contrato.getSucursalRecojo(), contrato.getSucursalDevolucion());
            return ContratoResponse.de(contrato);
        }

        ReservaInfo reserva = reservaClient.obtener(idReserva);
        if (!"EN_CURSO".equals(reserva.estado())) {
            throw ContratoException.reservaNoDisponible(
                    "Solo se puede crear un contrato para una reserva EN_CURSO");
        }
        if (reserva.placaAsignada() == null || reserva.idCliente() == null) {
            throw ContratoException.reservaNoDisponible(
                    "La reserva debe tener un cliente y un vehículo asignado");
        }
        autorizarSucursal(rol, sucursal, reserva.sucursalRecojo(), reserva.sucursalDevolucion());

        String registradaPor = usuario == null || usuario.isBlank()
                ? "DESCONOCIDO"
                : usuario.trim();
        if (registradaPor.length() > 120) {
            registradaPor = registradaPor.substring(0, 120);
        }

        Contrato contrato = new Contrato(
                UUID.randomUUID(),
                reserva.idReserva(),
                reserva.idCliente(),
                reserva.placaAsignada(),
                reserva.sucursalRecojo(),
                reserva.sucursalDevolucion(),
                reserva.fechaInicio(),
                reserva.fechaFin(),
                registradaPor);
        try {
            return ContratoResponse.de(repository.saveAndFlush(contrato));
        } catch (DataIntegrityViolationException ex) {
            return repository.findByIdReserva(idReserva)
                    .map(ContratoResponse::de)
                    .orElseThrow(() -> ex);
        }
    }

    @Transactional(readOnly = true)
    public PaginaContratosResponse listar(
            UUID idReserva,
            UUID idCliente,
            String placa,
            EstadoContrato estado,
            int pagina,
            int tamano,
            String rol,
            String sucursal) {
        exigirAcceso(rol);
        int paginaSegura = Math.max(0, pagina);
        int tamanoSeguro = Math.min(Math.max(1, tamano), TAMANO_MAXIMO);

        Specification<Contrato> filtro = (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (idReserva != null) condiciones.add(cb.equal(root.get("idReserva"), idReserva));
            if (idCliente != null) condiciones.add(cb.equal(root.get("idCliente"), idCliente));
            if (placa != null && !placa.isBlank()) {
                condiciones.add(cb.equal(root.get("placa"), placa.trim().toUpperCase()));
            }
            if (estado != null) condiciones.add(cb.equal(root.get("estado"), estado));
            if (requiereSucursal(rol)) {
                if (sucursal == null || sucursal.isBlank()) {
                    throw ContratoException.sucursalNoAutorizada();
                }
                String sede = sucursal.trim().toUpperCase();
                condiciones.add(cb.or(
                        cb.equal(root.get("sucursalRecojo"), sede),
                        cb.equal(root.get("sucursalDevolucion"), sede)));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };

        Page<Contrato> resultado = repository.findAll(
                filtro,
                PageRequest.of(paginaSegura, tamanoSeguro,
                        Sort.by(Sort.Direction.DESC, "creadoEn")));
        return new PaginaContratosResponse(
                resultado.getTotalElements(),
                paginaSegura,
                tamanoSeguro,
                resultado.getTotalPages(),
                resultado.getContent().stream().map(ContratoResponse::de).toList());
    }

    @Transactional(readOnly = true)
    public ContratoResponse obtener(UUID id, String rol, String sucursal) {
        exigirAcceso(rol);
        Contrato contrato = repository.findById(id)
                .orElseThrow(ContratoException::contratoNoEncontrado);
        autorizarSucursal(rol, sucursal,
                contrato.getSucursalRecojo(), contrato.getSucursalDevolucion());
        return ContratoResponse.de(contrato);
    }

    @Transactional(readOnly = true)
    public ContratoResponse obtenerPorReserva(UUID idReserva, String rol, String sucursal) {
        exigirAcceso(rol);
        Contrato contrato = repository.findByIdReserva(idReserva)
                .orElseThrow(ContratoException::contratoNoEncontrado);
        autorizarSucursal(rol, sucursal,
                contrato.getSucursalRecojo(), contrato.getSucursalDevolucion());
        return ContratoResponse.de(contrato);
    }

    /**
     * Finaliza el contrato al terminar la reserva. Se puede repetir de forma segura.
     */
    public ContratoResponse finalizarPorReserva(
            UUID idReserva, String rol, String sucursal) {
        exigirAcceso(rol);
        Contrato contrato = repository.findByIdReserva(idReserva)
                .orElseThrow(ContratoException::contratoNoEncontrado);
        autorizarSucursal(rol, sucursal,
                contrato.getSucursalRecojo(), contrato.getSucursalDevolucion());

        if (contrato.getEstado() == EstadoContrato.FINALIZADO) {
            return ContratoResponse.de(contrato);
        }

        ReservaInfo reserva = reservaClient.obtener(idReserva);
        if (!"FINALIZADA".equals(reserva.estado())) {
            throw ContratoException.transicionInvalida(
                    "La reserva debe estar FINALIZADA antes de cerrar el contrato");
        }

        LocalDate fechaDevolucion = LocalDate.now(ZONA_LIMA);
        if (fechaDevolucion.isBefore(contrato.getFechaInicio())) {
            throw ContratoException.transicionInvalida(
                    "La fecha de devolución no puede ser anterior al inicio del contrato");
        }
        contrato.finalizar(fechaDevolucion);
        return ContratoResponse.de(repository.save(contrato));
    }

    private void exigirAcceso(String rol) {
        String normalizado = rol == null ? "" : rol.trim().toUpperCase();
        if (!ROLES_PERSONAL.contains(normalizado) && !ROL_SERVICIO.equals(normalizado)) {
            throw ContratoException.rolNoAutorizado();
        }
    }

    private boolean requiereSucursal(String rol) {
        String normalizado = rol == null ? "" : rol.trim().toUpperCase();
        return !ROL_ADMINISTRADOR.equals(normalizado) && !ROL_SERVICIO.equals(normalizado);
    }

    private void autorizarSucursal(
            String rol, String sucursalUsuario, String sucursalRecojo, String sucursalDevolucion) {
        if (!requiereSucursal(rol)) return;
        if (sucursalUsuario == null || sucursalUsuario.isBlank()) {
            throw ContratoException.sucursalNoAutorizada();
        }
        String sede = sucursalUsuario.trim();
        if (!sede.equalsIgnoreCase(sucursalRecojo)
                && !sede.equalsIgnoreCase(sucursalDevolucion)) {
            throw ContratoException.sucursalNoAutorizada();
        }
    }
}
