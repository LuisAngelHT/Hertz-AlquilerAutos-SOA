package com.hertz.reserva.client;

import com.hertz.reserva.exception.ReservaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Registra en ServicioIncidencia los daños que el cliente causó durante un alquiler. Esa incidencia es la que manda
 * el vehículo a MANTENIMIENTO (lo hace ServicioIncidencia con ServicioFlota) hasta que se repare y se cierre.
 */
@Component
public class IncidenciaClient {

    private static final Logger log = LoggerFactory.getLogger(IncidenciaClient.class);
    private static final String SERVICIO = "ServicioIncidencia";
    private static final Pattern MENSAJE = Pattern.compile("\"mensaje\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private final RestClient http;

    public IncidenciaClient(RestClient incidenciaRestClient) {
        this.http = incidenciaRestClient;
    }

    /**
     * Crea la incidencia del contrato y devuelve true; si ese contrato ya tiene una incidencia registrada (por
     * ejemplo en un reintento), no crea otra y devuelve false. Así repetir la devolución nunca duplica nada.
     *
     * @param tipo DANO o SINIESTRO
     */
    public boolean registrarDanos(String placa, UUID idContrato, String tipo, String descripcion,
                                  BigDecimal costoEstimado, String registradoPor) {
        if (idContrato != null && yaRegistrada(placa, idContrato)) {
            return false;
        }
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("idVehiculo", placa);
        cuerpo.put("tipo", tipo);
        cuerpo.put("descripcion", descripcion);
        if (idContrato != null) {
            cuerpo.put("idContrato", idContrato);
        }
        if (costoEstimado != null) {
            cuerpo.put("costoEstimado", costoEstimado);
        }
        llamar(() -> http.post()
                .uri("/api/v1/incidencia")
                .headers(h -> {
                    if (registradoPor != null && !registradoPor.isBlank()) {
                        h.set("X-Usuario", registradoPor.trim());
                    }
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo)
                .retrieve()
                .toBodilessEntity());
        return true;
    }

    private boolean yaRegistrada(String placa, UUID idContrato) {
        Map<String, Object>[] pagina = new Map[1];
        llamar(() -> pagina[0] = http.get()
                .uri(u -> u.path("/api/v1/incidencia").queryParam("idVehiculo", placa).queryParam("tamano", 50).build())
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() { }));
        Object lista = pagina[0] == null ? null : pagina[0].get("incidencias");
        if (lista instanceof List<?> filas) {
            for (Object fila : filas) {
                if (fila instanceof Map<?, ?> incidencia && idContrato.toString().equals(String.valueOf(incidencia.get("idContrato")))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void llamar(Runnable operacion) {
        for (int intento = 1; intento <= 2; intento++) {
            try {
                operacion.run();
                return;
            } catch (RestClientResponseException ex) {
                if (!ex.getStatusCode().is5xxServerError()) {
                    String motivo = mensajeDe(ex);
                    throw ReservaException.solicitudInvalida(motivo != null ? motivo
                            : SERVICIO + " rechazó la incidencia (HTTP " + ex.getStatusCode().value() + ")");
                }
                log.warn("{} respondió HTTP {} (intento {})", SERVICIO, ex.getStatusCode().value(), intento);
            } catch (RestClientException ex) {
                log.warn("{} no respondió (intento {}): {}", SERVICIO, intento, ex.getMessage());
            }
        }
        throw ReservaException.servicioNoDisponible(SERVICIO, "intenta nuevamente en unos segundos");
    }

    static String mensajeDe(RestClientResponseException ex) {
        String cuerpo = ex.getResponseBodyAsString();
        if (cuerpo == null || cuerpo.isBlank()) {
            return null;
        }
        Matcher m = MENSAJE.matcher(cuerpo);
        return m.find() ? m.group(1).replace("\\\"", "\"") : null;
    }
}
