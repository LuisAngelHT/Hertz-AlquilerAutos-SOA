package com.hertz.reserva.client;

import com.hertz.reserva.dto.DevolucionDto;
import com.hertz.reserva.dto.EntregaDto;
import com.hertz.reserva.exception.ReservaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ContratoClient {

    private static final Logger log = LoggerFactory.getLogger(ContratoClient.class);
    private static final String SERVICIO = "ServicioContrato";
    private static final Pattern MENSAJE = Pattern.compile("\"mensaje\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private final RestClient http;

    public ContratoClient(RestClient contratoRestClient) {
        this.http = contratoRestClient;
    }

    /**
     * Crea el contrato de la reserva con la revision del vehiculo al salir. {@code registradoPor} es el correo
     * del empleado de Hertz que hizo la entrega: queda guardado en el contrato (si viene vacío, el contrato
     * usa la identidad del servicio). Repetirlo es seguro: si el contrato ya existe, lo devuelve.
     */
    public void crear(UUID idReserva, String registradoPor, EntregaDto entrega) {
        llamar(() -> http.post()
                .uri("/api/v1/contrato")
                .headers(h -> {
                    if (registradoPor != null && !registradoPor.isBlank()) {
                        h.set("X-Usuario", registradoPor.trim());
                    }
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("idReserva", idReserva, "entrega", entrega, "contratoFirmado", true))
                .retrieve()
                .toBodilessEntity());
    }

    /**
     * Cierra el contrato con la revision del vehiculo al volver. Repetirlo es seguro. Devuelve el id del contrato
     * (o null si la respuesta no lo trae), que se usa para enlazar la incidencia de los daños.
     */
    public UUID finalizar(UUID idReserva, String registradoPor, DevolucionDto devolucion) {
        Map<String, Object>[] respuesta = new Map[1];
        llamar(() -> respuesta[0] = http.put()
                .uri("/api/v1/contrato/reserva/{idReserva}/finalizar", idReserva)
                .headers(h -> {
                    if (registradoPor != null && !registradoPor.isBlank()) {
                        h.set("X-Usuario", registradoPor.trim());
                    }
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(devolucion)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() { }));
        Object id = respuesta[0] == null ? null : respuesta[0].get("idContrato");
        try {
            return id == null ? null : UUID.fromString(String.valueOf(id));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void llamar(Runnable operacion) {
        for (int intento = 1; intento <= 2; intento++) {
            try {
                operacion.run();
                return;
            } catch (RestClientResponseException ex) {
                if (!ex.getStatusCode().is5xxServerError()) {
                    // Un 4xx es un rechazo por una regla: se muestra el motivo que dio el contrato, no un codigo.
                    String motivo = mensajeDe(ex);
                    if (motivo != null) {
                        throw ReservaException.solicitudInvalida(motivo);
                    }
                    throw ReservaException.transicionInvalida(
                            SERVICIO + " rechazo la operación (HTTP "
                                    + ex.getStatusCode().value() + ")");
                }
                log.warn("{} respondió HTTP {} (intento {})",
                        SERVICIO, ex.getStatusCode().value(), intento);
            } catch (RestClientException ex) {
                log.warn("{} no respondió (intento {}): {}", SERVICIO, intento, ex.getMessage());
            }
        }
        throw ReservaException.servicioNoDisponible(SERVICIO,
                "intenta nuevamente en unos segundos");
    }

    /** Extrae el "mensaje" del JSON de error del contrato ({"codigo":..., "mensaje":...}), si lo trae. */
    static String mensajeDe(RestClientResponseException ex) {
        String cuerpo = ex.getResponseBodyAsString();
        if (cuerpo == null || cuerpo.isBlank()) {
            return null;
        }
        Matcher m = MENSAJE.matcher(cuerpo);
        return m.find() ? m.group(1).replace("\\\"", "\"") : null;
    }
}
