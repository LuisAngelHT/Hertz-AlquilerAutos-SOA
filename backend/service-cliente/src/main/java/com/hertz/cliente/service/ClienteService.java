package com.hertz.cliente.service;

import com.hertz.cliente.client.AuthClient;
import com.hertz.cliente.dto.ActualizarClienteRequest;
import com.hertz.cliente.dto.ActualizarPerfilRequest;
import com.hertz.cliente.dto.ClienteRequest;
import com.hertz.cliente.dto.PaginaClientesResponse;
import com.hertz.cliente.dto.ClienteResponse;
import com.hertz.cliente.entity.Cliente;
import com.hertz.cliente.entity.TipoDocumento;
import com.hertz.cliente.exception.ClienteException;
import com.hertz.cliente.repository.ClienteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ClienteService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final Pattern DNI = Pattern.compile("\\d{8}");
    private static final Pattern CE = Pattern.compile("[A-Za-z0-9]{9,12}");
    private static final Pattern PASAPORTE = Pattern.compile("[A-Za-z0-9]{6,12}");
    private static final Set<String> ROLES_GESTION = Set.of("AGENTE", "ADMINISTRADOR", "ADMINISTRADOR_SUCURSAL");

    // Rol interno de ServicioReserva: puede consultar clientes (no registrarlos ni modificarlos).
    private static final String ROL_SERVICIO = "SERVICIO";

    private static final int EDAD_MINIMA = 18;
    static final int TAMANO_POR_DEFECTO = 10;
    static final int TAMANO_MAXIMO = 50;

    private final ClienteRepository repository;
    private final AuthClient authClient;

    public ClienteService(ClienteRepository repository, AuthClient authClient) {
        this.repository = repository;
        this.authClient = authClient;
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscar(TipoDocumento tipo, String numero, String rol) {
        exigirConsulta(rol);
        String documento = normalizar(numero);
        validarDocumento(tipo, documento);
        return repository.findByTipoDocumentoAndNumeroDocumento(tipo, documento)
                .map(this::aRespuesta)
                .orElseThrow(ClienteException::clienteNoEncontrado);
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtener(UUID id, String rol) {
        exigirConsulta(rol);
        return repository.findById(id)
                .map(this::aRespuesta)
                .orElseThrow(ClienteException::clienteNoEncontrado);
    }

    /** Perfil del cliente dueño de un correo. Solo lo usa ServicioReserva (rol SERVICIO) para el cliente logueado. */
    @Transactional(readOnly = true)
    public ClienteResponse buscarPorCorreo(String correo, String rol) {
        if (!ROL_SERVICIO.equals(rol)) {
            throw ClienteException.rolNoPermitido();
        }
        return repository.findByEmailIgnoreCase(correo == null ? "" : correo.trim())
                .map(this::aRespuesta)
                .orElseThrow(ClienteException::clienteNoEncontrado);
    }

    // Registro presencial: solo AGENTE o ADMINISTRADOR
    public ClienteResponse registrarPresencial(ClienteRequest request, String rol) {
        exigirGestion(rol);
        return registrar(request);
    }

    /**
     * Registro (virtual o presencial): crea el perfil y luego las credenciales en Autenticación.
     * No es @Transactional a propósito: el perfil se confirma antes de llamar a Auth y,
     * si Auth falla, se compensa borrándolo.
     */
    public ClienteResponse registrar(ClienteRequest request) {
        String documento = normalizar(request.numeroDocumento());
        validarDocumento(request.tipoDocumento(), documento);
        String email = request.email().trim().toLowerCase();
        validarEdad(request.fechaNacimiento());
        validarLicenciaVigente(request.vencimientoLicencia());

        if (repository.existsByTipoDocumentoAndNumeroDocumento(request.tipoDocumento(), documento)) {
            throw ClienteException.clienteDuplicado();
        }
        if (repository.existsByEmailIgnoreCase(email)) {
            throw ClienteException.correoYaRegistrado();
        }

        Cliente guardado = repository.save(new Cliente(
                request.tipoDocumento(), documento,
                request.nombres().trim(), request.apellidos().trim(),
                request.fechaNacimiento(), email,
                request.telefono(), request.licenciaConducir(), request.vencimientoLicencia()));

        try {
            authClient.crearCredenciales(email, nombreCompleto(guardado), request.password(), guardado.getId());
        } catch (RuntimeException ex) {
            repository.delete(guardado);   // compensación: no dejar un perfil sin cuenta
            throw ex;
        }
        return aRespuesta(guardado);
    }

    @Transactional
    public ClienteResponse actualizar(UUID id, ActualizarClienteRequest request, String rol) {
        exigirGestion(rol);
        Cliente cliente = repository.findById(id).orElseThrow(ClienteException::clienteNoEncontrado);
        validarLicenciaVigente(request.vencimientoLicencia());
        cliente.actualizarDatos(
                request.nombres().trim(), request.apellidos().trim(),
                request.email().trim().toLowerCase(), request.telefono(),
                request.licenciaConducir(), request.vencimientoLicencia());
        return aRespuesta(repository.save(cliente));
    }

    /** Listado de todos los clientes, paginado y con búsqueda opcional por nombre, documento o correo. Solo personal. */
    @Transactional(readOnly = true)
    public PaginaClientesResponse listar(String texto, Integer pagina, Integer tamano, String rol) {
        exigirGestion(rol);
        String patron = texto == null || texto.isBlank() ? "%" : "%" + texto.trim().toLowerCase() + "%";
        int paginaPedida = pagina == null ? 0 : Math.max(0, pagina);
        int tamanoPedido = tamano == null ? TAMANO_POR_DEFECTO : Math.min(TAMANO_MAXIMO, Math.max(1, tamano));
        Page<Cliente> resultado = repository.buscar(patron, PageRequest.of(paginaPedida, tamanoPedido,
                Sort.by("apellidos", "nombres")));
        return new PaginaClientesResponse(resultado.getTotalElements(), paginaPedida, tamanoPedido,
                resultado.getTotalPages(), resultado.getContent().stream().map(this::aRespuesta).toList());
    }

    /** El perfil del cliente que inició sesión (se identifica por el correo del token). Solo rol CLIENTE. */
    @Transactional(readOnly = true)
    public ClienteResponse perfilPropio(String correo, String rol) {
        return aRespuesta(clientePropio(correo, rol));
    }

    /** El cliente actualiza su teléfono y su licencia (la licencia debe estar vigente). */
    @Transactional
    public ClienteResponse actualizarPerfilPropio(String correo, String rol, ActualizarPerfilRequest request) {
        Cliente cliente = clientePropio(correo, rol);
        validarLicenciaVigente(request.vencimientoLicencia());
        cliente.actualizarContacto(request.telefono().trim(), request.licenciaConducir().trim(),
                request.vencimientoLicencia());
        return aRespuesta(repository.save(cliente));
    }

    private Cliente clientePropio(String correo, String rol) {
        if (!"CLIENTE".equals(rol)) {
            throw ClienteException.soloCliente();
        }
        return repository.findByEmailIgnoreCase(correo == null ? "" : correo.trim())
                .orElseThrow(ClienteException::clienteNoEncontrado);
    }

    // Perú: se necesita ser mayor de 18 años para tener licencia y alquilar.
    private void validarEdad(LocalDate fechaNacimiento) {
        if (fechaNacimiento.isAfter(LocalDate.now(LIMA).minusYears(EDAD_MINIMA))) {
            throw ClienteException.menorDeEdad();
        }
    }

    private void validarLicenciaVigente(LocalDate vencimiento) {
        if (vencimiento != null && vencimiento.isBefore(LocalDate.now(LIMA))) {
            throw ClienteException.licenciaVencida();
        }
    }

    private void exigirConsulta(String rol) {
        if (!ROLES_GESTION.contains(rol) && !ROL_SERVICIO.equals(rol)) {
            throw ClienteException.rolNoPermitido();
        }
    }

    private void exigirGestion(String rol) {
        if (!ROLES_GESTION.contains(rol)) {
            throw ClienteException.rolNoPermitido();
        }
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

    private String nombreCompleto(Cliente c) {
        String nombre = c.getNombres() + " " + c.getApellidos();
        return nombre.length() > 100 ? nombre.substring(0, 100) : nombre;
    }

    private ClienteResponse aRespuesta(Cliente c) {
        boolean licenciaVigente = c.getVencimientoLicencia() != null
                && !c.getVencimientoLicencia().isBefore(LocalDate.now(LIMA));
        return new ClienteResponse(c.getId(), c.getTipoDocumento(), c.getNumeroDocumento(),
                c.getNombres(), c.getApellidos(), c.getFechaNacimiento(), c.getEmail(),
                c.getTelefono(), c.getLicenciaConducir(), c.getVencimientoLicencia(),
                licenciaVigente, List.of());
    }
}