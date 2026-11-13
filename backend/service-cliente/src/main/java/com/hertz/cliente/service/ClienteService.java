package com.hertz.cliente.service;

import com.hertz.cliente.dto.ActualizarClienteRequest;
import com.hertz.cliente.dto.ClienteRequest;
import com.hertz.cliente.dto.ClienteResponse;
import com.hertz.cliente.entity.Cliente;
import com.hertz.cliente.entity.TipoDocumento;
import com.hertz.cliente.exception.ClienteException;
import com.hertz.cliente.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ClienteService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final Pattern DNI = Pattern.compile("\\d{8}");
    private static final Pattern CE = Pattern.compile("[A-Za-z0-9]{9,12}");
    private static final Pattern PASAPORTE = Pattern.compile("[A-Za-z0-9]{6,12}");

    private final ClienteRepository repository;

    public ClienteService(ClienteRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscar(TipoDocumento tipo, String numero) {
        String documento = normalizar(numero);
        validarDocumento(tipo, documento);
        return repository.findByTipoDocumentoAndNumeroDocumento(tipo, documento)
                .map(this::aRespuesta)
                .orElseThrow(ClienteException::clienteNoEncontrado);
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtener(UUID id) {
        return repository.findById(id)
                .map(this::aRespuesta)
                .orElseThrow(ClienteException::clienteNoEncontrado);
    }

    @Transactional
    public ClienteResponse registrar(ClienteRequest request) {
        String documento = normalizar(request.numeroDocumento());
        validarDocumento(request.tipoDocumento(), documento);

        if (repository.existsByTipoDocumentoAndNumeroDocumento(request.tipoDocumento(), documento)) {
            throw ClienteException.clienteDuplicado();
        }

        Cliente cliente = new Cliente(
                request.tipoDocumento(), documento,
                request.nombres().trim(), request.apellidos().trim(),
                request.fechaNacimiento(), request.email().trim().toLowerCase(),
                request.telefono(), request.licenciaConducir(), request.vencimientoLicencia());
        return aRespuesta(repository.save(cliente));
    }

    @Transactional
    public ClienteResponse actualizar(UUID id, ActualizarClienteRequest request) {
        Cliente cliente = repository.findById(id).orElseThrow(ClienteException::clienteNoEncontrado);
        cliente.actualizarDatos(
                request.nombres().trim(), request.apellidos().trim(),
                request.email().trim().toLowerCase(), request.telefono(),
                request.licenciaConducir(), request.vencimientoLicencia());
        return aRespuesta(repository.save(cliente));
    }

    private String normalizar(String numero) {
        return numero == null ? "" : numero.trim().toUpperCase();
    }

    // Reglas del proyecto: DNI de 8 dígitos (según la ficha de diseño); CE y PASAPORTE son ajustables
    private void validarDocumento(TipoDocumento tipo, String documento) {
        boolean valido = switch (tipo) {
            case DNI -> DNI.matcher(documento).matches();
            case CE -> CE.matcher(documento).matches();
            case PASAPORTE -> PASAPORTE.matcher(documento).matches();
        };
        if (!valido) {
            throw ClienteException.documentoInvalido("El documento no tiene un formato válido para " + tipo
                    + (tipo == TipoDocumento.DNI ? " (8 dígitos numéricos)" : ""));
        }
    }

    private ClienteResponse aRespuesta(Cliente c) {
        boolean licenciaVigente = c.getVencimientoLicencia() != null
                && !c.getVencimientoLicencia().isBefore(LocalDate.now(LIMA));
        return new ClienteResponse(c.getId(), c.getTipoDocumento(), c.getNumeroDocumento(),
                c.getNombres(), c.getApellidos(), c.getEmail(), c.getTelefono(),
                licenciaVigente, List.of());
    }
}