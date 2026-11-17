package com.hertz.cliente.client;

import com.hertz.cliente.exception.ClienteException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.UUID;

@Component
public class AuthClient {

    private final RestClient restClient;

    public AuthClient(RestClient authRestClient) {
        this.restClient = authRestClient;
    }

    public void crearCredenciales(String correo, String nombre, String password, UUID clienteId) {
        try {
            restClient.post()
                    .uri("/api/v1/auth/usuarios")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("correo", correo, "nombre", nombre,
                            "password", password, "clienteId", clienteId.toString()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 409) {
                throw ClienteException.correoYaRegistrado();
            }
            throw ClienteException.servicioAuthNoDisponible();
        } catch (RestClientException ex) {
            throw ClienteException.servicioAuthNoDisponible();
        }
    }
}