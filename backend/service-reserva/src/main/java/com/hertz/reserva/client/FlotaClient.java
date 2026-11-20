package com.hertz.reserva.client;

import com.hertz.reserva.exception.ReservaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Cliente de ServicioFlota (SVC-03): aparta, reprograma y libera el vehículo de una reserva.
 * La "referencia" es el id de la reserva. Las tres operaciones son idempotentes en flota, así
 * que ante un corte de red se reintenta una vez sin riesgo de apartar dos vehículos.
 */
@Component
public class FlotaClient {

    private static final Logger log = LoggerFactory.getLogger(FlotaClient.class);
    private static final String SERVICIO = "ServicioFlota";

    /** El vehículo que quedó apartado. */
    public record Apartado(String referencia, String placa) {
    }

    private final RestClient http;

    public FlotaClient(RestClient flotaRestClient) {
        this.http = flotaRestClient;
    }

    /** Aparta un vehículo libre de la categoría y sucursal en el rango de fechas. 409 -> SIN_DISPONIBILIDAD. */
    public Apartado apartar(String referencia, String sucursal, String categoria, LocalDate inicio, LocalDate fin,
                            String placa) {
        Map<String, Object> cuerpo = new java.util.HashMap<>(cuerpo(referencia, sucursal, categoria, inicio, fin));
        if (placa != null && !placa.isBlank()) {
            cuerpo.put("placa", placa);   // vehiculo concreto elegido por el cliente
        }
        return llamar(() -> http.post().uri("/api/v1/flota/bloqueos")
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo)
                .retrieve().body(Apartado.class));
    }

    /** Cambia fechas/categoría del apartado en una sola operación atómica de flota. */
    public Apartado reprogramar(String referencia, String sucursal, String categoria, LocalDate inicio, LocalDate fin) {
        return llamar(() -> http.put().uri("/api/v1/flota/bloqueos/{ref}", referencia)
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo(referencia, sucursal, categoria, inicio, fin))
                .retrieve().body(Apartado.class));
    }

    /** Libera el vehículo apartado (no falla si ya estaba libre). */
    public void liberar(String referencia) {
        llamar(() -> {
            http.delete().uri("/api/v1/flota/bloqueos/{ref}", referencia).retrieve().toBodilessEntity();
            return null;
        });
    }

    /** Vehículo tal como lo devuelve flota (solo lo que reserva necesita). */
    public record Vehiculo(String placa, String estado) {
    }

    /** Estado actual del vehículo en flota (DISPONIBLE, ALQUILADO, MANTENIMIENTO, FUERA_DE_SERVICIO). */
    public String consultarEstado(String placa) {
        Vehiculo vehiculo = llamar(() -> http.get().uri("/api/v1/flota/vehiculos/{placa}", placa)
                .retrieve().body(Vehiculo.class));
        return vehiculo == null ? null : vehiculo.estado();
    }

    /**
     * Check-out: marca el vehículo ALQUILADO en flota (según el documento, antes de crear el contrato).
     * Si ya estaba ALQUILADO no hace nada, así reintentar la entrega es seguro. Si está en
     * mantenimiento o dado de baja no se puede entregar: 409 con un mensaje claro.
     */
    public void marcarAlquilado(String placa, String motivo) {
        String actual = consultarEstado(placa);
        if ("ALQUILADO".equals(actual)) {
            return;
        }
        if (!"DISPONIBLE".equals(actual)) {
            throw ReservaException.transicionInvalida("El vehículo " + placa + " está en " + actual
                    + " y no se puede entregar. Cambia la reserva a otro vehículo de la categoría.");
        }
        cambiarEstado(placa, "ALQUILADO", motivo);
    }

    /**
     * Devolución: el vehículo vuelve a DISPONIBLE solo si estaba ALQUILADO. Si mientras tanto pasó a
     * MANTENIMIENTO (una incidencia) o a baja, se respeta ese estado.
     */
    public void marcarDevuelto(String placa, String motivo) {
        if ("ALQUILADO".equals(consultarEstado(placa))) {
            cambiarEstado(placa, "DISPONIBLE", motivo);
        }
    }

    private void cambiarEstado(String placa, String estado, String motivo) {
        llamar(() -> {
            http.put().uri("/api/v1/flota/vehiculos/{placa}/estado", placa)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("estado", estado, "motivo", motivo))
                    .retrieve().toBodilessEntity();
            return null;
        });
    }

    private static Map<String, Object> cuerpo(String referencia, String sucursal, String categoria,
                                              LocalDate inicio, LocalDate fin) {
        return Map.of("referencia", referencia, "sucursal", sucursal, "categoria", categoria,
                "fechaInicio", inicio.toString(), "fechaFin", fin.toString());
    }

    // Ejecuta la llamada con un reintento ante fallos de red o errores 5xx, y traduce los errores.
    private <T> T llamar(Supplier<T> operacion) {
        RuntimeException ultimo = null;
        for (int intento = 1; intento <= 2; intento++) {
            try {
                return operacion.get();
            } catch (RestClientResponseException ex) {
                if (ex.getStatusCode().is5xxServerError()) {
                    ultimo = ex;
                    log.warn("{} respondio {} (intento {})", SERVICIO, ex.getStatusCode().value(), intento);
                    continue;
                }
                throw traducir(ex);
            } catch (RestClientException ex) {
                ultimo = ex;
                log.warn("{} no respondio (intento {}): {}", SERVICIO, intento, ex.getMessage());
            }
        }
        throw ReservaException.servicioNoDisponible(SERVICIO, "intenta nuevamente en unos segundos");
    }

    private ReservaException traducir(RestClientResponseException ex) {
        String cuerpo = ex.getResponseBodyAsString();
        if (ex.getStatusCode().value() == 409 && cuerpo.contains("VEHICULO_NO_DISPONIBLE")) {
            return ReservaException.vehiculoElegidoNoDisponible();
        }
        if (ex.getStatusCode().value() == 409 && cuerpo.contains("SIN_DISPONIBILIDAD")) {
            return ReservaException.sinDisponibilidad(
                    "No hay vehiculos de esa categoria disponibles en esas fechas y sucursal");
        }
        if (ex.getStatusCode().value() == 404) {
            return ReservaException.noEncontrada();
        }
        if (ex.getStatusCode().value() == 400) {
            return ReservaException.solicitudInvalida("Datos de reserva invalidos segun ServicioFlota");
        }
        log.error("{} rechazo la solicitud: {} {}", SERVICIO, ex.getStatusCode().value(), cuerpo);
        return ReservaException.servicioNoDisponible(SERVICIO, "respuesta inesperada (" + ex.getStatusCode().value() + ")");
    }
}
