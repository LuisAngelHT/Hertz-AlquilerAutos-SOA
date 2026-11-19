package com.hertz.reserva.client;

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

@Component
public class ContratoClient {

    private static final Logger log = LoggerFactory.getLogger(ContratoClient.class);
    private static final String SERVICIO = "ServicioContrato";

    private final RestClient http;

    public ContratoClient(RestClient contratoRestClient) {
        this.http = contratoRestClient;
    }

    public void crear(UUID idReserva) {
        llamar(() -> http.post()
                .uri("/api/v1/contrato")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("idReserva", idReserva))
                .retrieve()
                .toBodilessEntity());
    }

    public void finalizar(UUID idReserva) {
        llamar(() -> http.put()
                .uri("/api/v1/contrato/reserva/{idReserva}/finalizar", idReserva)
                .retrieve()
                .toBodilessEntity());
    }

    private void llamar(Runnable operacion) {
        for (int intento = 1; intento <= 2; intento++) {
            try {
                operacion.run();
                return;
            } catch (RestClientResponseException ex) {
                if (!ex.getStatusCode().is5xxServerError()) {
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
}
