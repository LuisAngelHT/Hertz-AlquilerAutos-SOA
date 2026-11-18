package com.hertz.reserva.client;

import com.hertz.reserva.exception.ReservaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Cliente de ServicioCliente (SVC-01): comprueba que el cliente existe (precondición 1 del
 * documento) y resuelve a un cliente por su documento o por su correo (para el cliente logueado).
 */
@Component
public class ClienteClient {

    private static final Logger log = LoggerFactory.getLogger(ClienteClient.class);
    private static final String SERVICIO = "ServicioCliente";

    /** Los datos del cliente que interesan a reservas (el resto se ignora). */
    public record ClienteInfo(UUID idCliente, String tipoDocumento, String numeroDocumento,
                              String nombres, String apellidos, String email, String telefono,
                              String licenciaConducir, LocalDate vencimientoLicencia) {

        public String nombreCompleto() {
            return (nombres + " " + apellidos).trim();
        }

        public String documento() {
            return tipoDocumento + " " + numeroDocumento;
        }
    }

    private final RestClient http;

    public ClienteClient(RestClient clienteRestClient) {
        this.http = clienteRestClient;
    }

    /** El cliente con ese id; 404 -> CLIENTE_NO_ENCONTRADO. */
    public ClienteInfo obtener(UUID idCliente) {
        return llamar(() -> http.get().uri("/api/v1/cliente/{id}", idCliente).retrieve().body(ClienteInfo.class))
                .orElseThrow(ReservaException::clienteNoEncontrado);
    }

    /** El cliente dueño de ese correo (el usuario que inició sesión); 404 -> CLIENTE_NO_ENCONTRADO. */
    public ClienteInfo porCorreo(String correo) {
        return llamar(() -> http.get().uri(u -> u.path("/api/v1/cliente/correo").queryParam("email", correo).build())
                .retrieve().body(ClienteInfo.class))
                .orElseThrow(ReservaException::clienteNoEncontrado);
    }

    /** El cliente con ese documento, o vacío si no existe. */
    public Optional<ClienteInfo> porDocumento(String tipoDocumento, String numeroDocumento) {
        return llamar(() -> http.get().uri(u -> u.path("/api/v1/cliente")
                        .queryParam("tipoDocumento", tipoDocumento)
                        .queryParam("numeroDocumento", numeroDocumento).build())
                .retrieve().body(ClienteInfo.class));
    }

    // 404 -> Optional vacío; 400 -> documento inválido; fallos de red o 5xx -> un reintento y luego 503.
    private <T> Optional<T> llamar(Supplier<T> operacion) {
        for (int intento = 1; intento <= 2; intento++) {
            try {
                return Optional.ofNullable(operacion.get());
            } catch (RestClientResponseException ex) {
                int codigo = ex.getStatusCode().value();
                if (codigo == 404) {
                    return Optional.empty();
                }
                if (codigo == 400) {
                    throw ReservaException.solicitudInvalida("El documento no tiene un formato valido");
                }
                if (!ex.getStatusCode().is5xxServerError()) {
                    log.error("{} rechazo la solicitud: {} {}", SERVICIO, codigo, ex.getResponseBodyAsString());
                    throw ReservaException.servicioNoDisponible(SERVICIO, "respuesta inesperada (" + codigo + ")");
                }
                log.warn("{} respondio {} (intento {})", SERVICIO, codigo, intento);
            } catch (RestClientException ex) {
                log.warn("{} no respondio (intento {}): {}", SERVICIO, intento, ex.getMessage());
            }
        }
        throw ReservaException.servicioNoDisponible(SERVICIO, "intenta nuevamente en unos segundos");
    }
}
