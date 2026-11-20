package com.hertz.incidencia.client;

import com.hertz.incidencia.exception.IncidenciaException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
public class FlotaClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAPA = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;

    public FlotaClient(RestClient flotaRestClient) {
        this.restClient = flotaRestClient;
    }

    // Pre-condición 1: el vehículo existe en ServicioFlota
    public VehiculoInfo obtenerVehiculo(String placa) {
        try {
            Map<String, Object> cuerpo = restClient.get()
                    .uri("/api/v1/flota/vehiculos/{placa}", placa)
                    .retrieve()
                    .body(MAPA);
            if (cuerpo == null) {
                throw IncidenciaException.servicioNoDisponible("ServicioFlota");
            }
            return new VehiculoInfo(String.valueOf(cuerpo.get("placa")),
                    String.valueOf(cuerpo.get("estado")),
                    String.valueOf(cuerpo.get("sucursal")));
        } catch (RestClientResponseException ex) {
            throw traducir(ex);
        } catch (RestClientException ex) {
            throw IncidenciaException.servicioNoDisponible("ServicioFlota");
        }
    }

    // Post-condición: el vehículo pasa a MANTENIMIENTO mediante ServicioFlota
    public void pasarAMantenimiento(String placa, String motivo) {
        cambiarEstado(placa, "MANTENIMIENTO", motivo);
    }

    // Al cerrar la última incidencia abierta, el vehículo vuelve a estar disponible (también mediante flota)
    public void pasarADisponible(String placa, String motivo) {
        cambiarEstado(placa, "DISPONIBLE", motivo);
    }

    private void cambiarEstado(String placa, String estado, String motivo) {
        try {
            restClient.put()
                    .uri("/api/v1/flota/vehiculos/{placa}/estado", placa)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("estado", estado, "motivo", motivo))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw traducir(ex);
        } catch (RestClientException ex) {
            throw IncidenciaException.servicioNoDisponible("ServicioFlota");
        }
    }

    private IncidenciaException traducir(RestClientResponseException ex) {
        return switch (ex.getStatusCode().value()) {
            case 400, 404 -> IncidenciaException.vehiculoNoEncontrado();
            case 403 -> IncidenciaException.sucursalNoAutorizada();
            case 409 -> IncidenciaException.transicionInvalida();
            default -> IncidenciaException.servicioNoDisponible("ServicioFlota");
        };
    }
}