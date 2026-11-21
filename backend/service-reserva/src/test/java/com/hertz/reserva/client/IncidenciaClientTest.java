package com.hertz.reserva.client;

import com.hertz.reserva.exception.ReservaException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La incidencia de los daños se crea una sola vez y con los datos del contrato. */
class IncidenciaClientTest {

    /** Servidor local: responde al listado (GET) y al registro (POST) y guarda lo que recibe. */
    private static final class Servidor implements AutoCloseable {
        final HttpServer http;
        final AtomicReference<String> cuerpoPost = new AtomicReference<>();
        final AtomicReference<String> usuarioPost = new AtomicReference<>();
        final AtomicReference<String> consulta = new AtomicReference<>();

        Servidor(String listadoJson, int estadoPost, String respuestaPost) throws Exception {
            http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            http.createContext("/", exchange -> {
                boolean esPost = "POST".equals(exchange.getRequestMethod());
                int estado = esPost ? estadoPost : 200;
                String json = esPost ? respuestaPost : listadoJson;
                if (esPost) {
                    cuerpoPost.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                    usuarioPost.set(exchange.getRequestHeaders().getFirst("X-Usuario"));
                } else {
                    consulta.set(exchange.getRequestURI().getQuery());
                }
                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(estado, bytes.length == 0 ? -1 : bytes.length);
                if (bytes.length > 0) exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            http.start();
        }

        IncidenciaClient cliente() {
            return new IncidenciaClient(RestClient.builder()
                    .baseUrl("http://127.0.0.1:" + http.getAddress().getPort())
                    .requestFactory(new JdkClientHttpRequestFactory(
                            HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()))
                    .defaultHeader("X-Usuario", "service-reserva").build());
        }

        @Override
        public void close() {
            http.stop(0);
        }
    }

    @Test
    void creaLaIncidenciaConLosDatosDelContratoYDelEmpleado() throws Exception {
        UUID contrato = UUID.randomUUID();
        try (Servidor s = new Servidor("{\"total\":0,\"incidencias\":[]}", 201, "{}")) {
            boolean creada = s.cliente().registrarDanos("ABC-104", contrato, "SINIESTRO",
                    "Daños causados en el alquiler", new BigDecimal("530.50"), "agente.lima@hertz.test");

            assertTrue(creada);
            assertTrue(s.consulta.get().contains("idVehiculo=ABC-104"), s.consulta.get());
            String json = s.cuerpoPost.get();
            assertEquals("agente.lima@hertz.test", s.usuarioPost.get());
            assertTrue(json.contains("\"idVehiculo\":\"ABC-104\""), json);
            assertTrue(json.contains("\"tipo\":\"SINIESTRO\""), json);
            assertTrue(json.contains("\"idContrato\":\"" + contrato + "\""), json);
            assertTrue(json.contains("\"costoEstimado\":530.50"), json);
        }
    }

    @Test
    void siElContratoYaTieneSuIncidenciaNoCreaOtra() throws Exception {
        UUID contrato = UUID.randomUUID();
        String listado = "{\"total\":2,\"incidencias\":[{\"idContrato\":null,\"tipo\":\"DAÑO\"},"
                + "{\"idContrato\":\"" + contrato + "\",\"tipo\":\"DAÑO\"}]}";
        try (Servidor s = new Servidor(listado, 201, "{}")) {
            assertFalse(s.cliente().registrarDanos("ABC-104", contrato, "DANO", "x", null, "a@hertz.test"));
            assertNull(s.cuerpoPost.get());   // no hubo ningun POST
        }
    }

    @Test
    void sinCostoNiContratoSeEnviaSoloLoNecesario() throws Exception {
        try (Servidor s = new Servidor("{}", 201, "{}")) {
            assertTrue(s.cliente().registrarDanos("ABC-104", null, "DANO", "Rayón", null, "a@hertz.test"));
            String json = s.cuerpoPost.get();
            assertFalse(json.contains("costoEstimado"), json);
            assertFalse(json.contains("idContrato"), json);
            assertNull(s.consulta.get());   // sin contrato no hay con que comparar: no consulta el listado
        }
    }

    @Test
    void siIncidenciasRechazaPorUnaReglaSeMuestraSuMotivo() throws Exception {
        String rechazo = "{\"codigo\":\"VEHICULO_ALQUILADO\",\"mensaje\":\"El vehículo ABC-104 está alquilado\"}";
        try (Servidor s = new Servidor("{\"incidencias\":[]}", 409, rechazo)) {
            ReservaException e = assertThrows(ReservaException.class,
                    () -> s.cliente().registrarDanos("ABC-104", UUID.randomUUID(), "DANO", "x", null, "a@hertz.test"));
            assertEquals("SOLICITUD_INVALIDA", e.getCodigo());
            assertEquals("El vehículo ABC-104 está alquilado", e.getMessage());
        }
    }

    @Test
    void siIncidenciasNoRespondeSePideReintentar() throws Exception {
        try (Servidor s = new Servidor("{\"incidencias\":[]}", 503, "")) {
            ReservaException e = assertThrows(ReservaException.class,
                    () -> s.cliente().registrarDanos("ABC-104", UUID.randomUUID(), "DANO", "x", null, "a@hertz.test"));
            assertEquals("SERVICIO_NO_DISPONIBLE", e.getCodigo());
        }
    }
}
