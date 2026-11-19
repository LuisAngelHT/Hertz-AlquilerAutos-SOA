package com.hertz.contrato.client;

import com.hertz.contrato.exception.ContratoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Component
public class ReservaClient {

    private static final Logger log = LoggerFactory.getLogger(ReservaClient.class);
    private final RestClient restClient;

    public ReservaClient(RestClient reservaRestClient) {
        this.restClient = reservaRestClient;
    }

    public ReservaInfo obtener(UUID idReserva) {
        RuntimeException ultimoError = null;
        for (int intento = 1; intento <= 2; intento++) {
            try {
                return restClient.get()
                        .uri("/api/v1/reserva/{id}", idReserva)
                        .retrieve()
                        .body(ReservaInfo.class);
            } catch (RestClientResponseException ex) {
                if (ex.getStatusCode().value() == 404) {
                    throw ContratoException.reservaNoEncontrada();
                }
                if (!ex.getStatusCode().is5xxServerError()) {
                    throw ContratoException.reservaNoDisponible(
                            "ServicioReserva respondió HTTP " + ex.getStatusCode().value());
                }
                ultimoError = ex;
                log.warn("ServicioReserva respondió HTTP {} (intento {})",
                        ex.getStatusCode().value(), intento);
            } catch (RestClientException ex) {
                ultimoError = ex;
                log.warn("ServicioReserva no respondió (intento {}): {}", intento, ex.getMessage());
            }
        }
        throw ContratoException.servicioNoDisponible("ServicioReserva", ultimoError);
    }
}
