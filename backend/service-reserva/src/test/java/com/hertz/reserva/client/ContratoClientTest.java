package com.hertz.reserva.client;

import com.hertz.reserva.dto.DanoDto;
import com.hertz.reserva.dto.DevolucionDto;
import com.hertz.reserva.dto.EntregaDto;
import com.hertz.reserva.exception.ReservaException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/** Comprueba que el contrato se registra a nombre del empleado y no de la identidad fija del servicio. */
class ContratoClientTest {

    private static final EntregaDto ENTREGA = new EntregaDto(12450, "LLENO",
            List.of(new DanoDto("FRONTAL", "Rayón en el parachoques", "LEVE", null)), "Sin novedades");

    private static final DevolucionDto DEVOLUCION = new DevolucionDto(12980, "MEDIO",
            List.of(new DanoDto("LATERAL_DERECHO", "Abolladura profunda", "MODERADO", new java.math.BigDecimal("120.50"))),
            null, true);

    /** Servidor local que guarda lo que recibe y responde con el estado y cuerpo indicados. */
    private static final class Eco implements AutoCloseable {
        final HttpServer servidor;
        final AtomicReference<String> cuerpo = new AtomicReference<>();
        final AtomicReference<String> metodo = new AtomicReference<>();
        final AtomicReference<String> usuario = new AtomicReference<>();

        Eco(int estado, String respuesta) throws Exception {
            servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            servidor.createContext("/", exchange -> {
                cuerpo.set(new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
                metodo.set(exchange.getRequestMethod());
                usuario.set(exchange.getRequestHeaders().getFirst("X-Usuario"));
                byte[] bytes = respuesta.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(estado, bytes.length == 0 ? -1 : bytes.length);
                if (bytes.length > 0) exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            servidor.start();
        }

        RestClient cliente() {
            return RestClient.builder().baseUrl("http://127.0.0.1:" + servidor.getAddress().getPort())
                    .requestFactory(new JdkClientHttpRequestFactory(
                            HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()))
                    .defaultHeader("X-Usuario", "service-reserva").build();
        }

        @Override
        public void close() {
            servidor.stop(0);
        }
    }

    @Test
    void laEntregaViajaAlContratoConLaRevisionYLaFirma() throws Exception {
        try (Eco eco = new Eco(201, "")) {
            new ContratoClient(eco.cliente()).crear(UUID.randomUUID(), "agente@hertz.test", ENTREGA);

            assertEquals("POST", eco.metodo.get());
            assertEquals("agente@hertz.test", eco.usuario.get());
            String json = eco.cuerpo.get();
            assertTrue(json.contains("\"contratoFirmado\":true"), json);
            assertTrue(json.contains("\"kilometraje\":12450"), json);
            assertTrue(json.contains("\"combustible\":\"LLENO\""), json);
            assertTrue(json.contains("\"zona\":\"FRONTAL\""), json);
        }
    }

    @Test
    void laUbicacionDelDanoYElInventarioViajanAlContratoSinElCampoInterno() throws Exception {
        var entrega = new EntregaDto(500, "LLENO",
                List.of(new DanoDto("LATERAL_IZQUIERDO", "Rayon", "LEVE", null, "IZQUIERDA",
                        new java.math.BigDecimal("42.50"), new java.math.BigDecimal("61.00"), "RAYON")),
                null, List.of(new com.hertz.reserva.dto.ItemDto("GATA", "OK", null, true)));
        try (Eco eco = new Eco(201, "")) {
            new ContratoClient(eco.cliente()).crear(UUID.randomUUID(), "a@hertz.test", entrega);

            String json = eco.cuerpo.get();
            assertTrue(json.contains("\"vista\":\"IZQUIERDA\""), json);
            assertTrue(json.contains("\"posX\":42.50"), json);
            assertTrue(json.contains("\"tipo\":\"RAYON\""), json);
            assertTrue(json.contains("\"inventario\":[{\"item\":\"GATA\",\"estado\":\"OK\""), json);
            assertTrue(!json.contains("nuevo"), json);   // "nuevo" es solo para armar la incidencia
        }
    }

    @Test
    void laDevolucionViajaAlContratoConLaRevisionYLoQueAsumeElCliente() throws Exception {
        try (Eco eco = new Eco(200, "")) {
            new ContratoClient(eco.cliente()).finalizar(UUID.randomUUID(), "cusco@hertz.test", DEVOLUCION);

            assertEquals("PUT", eco.metodo.get());
            assertEquals("cusco@hertz.test", eco.usuario.get());
            String json = eco.cuerpo.get();
            assertTrue(json.contains("\"kilometraje\":12980"), json);
            assertTrue(json.contains("\"clienteConforme\":true"), json);
            assertTrue(json.contains("\"costoEstimado\":120.50"), json);
        }
    }

    @Test
    void alCerrarElContratoDevuelveSuIdParaEnlazarLaIncidencia() throws Exception {
        UUID id = UUID.randomUUID();
        try (Eco eco = new Eco(200, "{\"idContrato\":\"" + id + "\",\"estado\":\"FINALIZADO\"}")) {
            assertEquals(id, new ContratoClient(eco.cliente()).finalizar(UUID.randomUUID(), "a@hertz.test", DEVOLUCION));
        }
        try (Eco sinCuerpo = new Eco(200, "")) {
            assertEquals(null, new ContratoClient(sinCuerpo.cliente()).finalizar(UUID.randomUUID(), "a@hertz.test", DEVOLUCION));
        }
    }

    @Test
    void siElContratoRechazaPorUnaReglaSeMuestraSuMotivo() throws Exception {
        String cuerpo = "{\"codigo\":\"REVISION_INVALIDA\",\"mensaje\":\"El kilometraje al devolver (10) no puede ser menor al de la entrega (20)\",\"timestamp\":\"2026-11-20T10:00:00Z\"}";
        try (Eco eco = new Eco(400, cuerpo)) {
            ReservaException e = assertThrows(ReservaException.class,
                    () -> new ContratoClient(eco.cliente()).finalizar(UUID.randomUUID(), "a@hertz.test", DEVOLUCION));

            assertEquals("SOLICITUD_INVALIDA", e.getCodigo());
            assertEquals("El kilometraje al devolver (10) no puede ser menor al de la entrega (20)", e.getMessage());
        }
    }

    @Test
    void sinMensajeLegibleSeUsaUnTextoGenerico() throws Exception {
        try (Eco eco = new Eco(409, "no es json")) {
            ReservaException e = assertThrows(ReservaException.class,
                    () -> new ContratoClient(eco.cliente()).crear(UUID.randomUUID(), "a@hertz.test", ENTREGA));
            assertEquals("TRANSICION_INVALIDA", e.getCodigo());
            assertTrue(e.getMessage().contains("409"), e.getMessage());
        }
    }

    @Test
    void siElContratoNoRespondeSePideReintentar() throws Exception {
        try (Eco eco = new Eco(503, "")) {
            ReservaException e = assertThrows(ReservaException.class,
                    () -> new ContratoClient(eco.cliente()).crear(UUID.randomUUID(), "a@hertz.test", ENTREGA));
            assertEquals("SERVICIO_NO_DISPONIBLE", e.getCodigo());
        }
    }

    private static RestClient.Builder constructorComoEnProduccion() {
        // Igual que RestClientsConfig: todas las llamadas llevan X-Usuario = service-reserva por defecto.
        return RestClient.builder()
                .baseUrl("http://contrato")
                .defaultHeader("X-Rol", "SERVICIO")
                .defaultHeader("X-Usuario", "service-reserva");
    }

    @Test
    void elContratoSeRegistraConElCorreoDelEmpleadoYSoloConEse() {
        RestClient.Builder builder = constructorComoEnProduccion();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://contrato/api/v1/contrato"))
                .andExpect(method(POST))
                .andExpect(header("X-Usuario", "agente@hertz.test"))   // un solo valor: el del empleado
                .andRespond(withStatus(CREATED));

        new ContratoClient(builder.build()).crear(UUID.randomUUID(), "agente@hertz.test", ENTREGA);

        servidor.verify();
    }

    @Test
    void sinEmpleadoConocidoSeUsaLaIdentidadDelServicio() {
        RestClient.Builder builder = constructorComoEnProduccion();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://contrato/api/v1/contrato"))
                .andExpect(header("X-Usuario", "service-reserva"))
                .andRespond(withStatus(CREATED));

        new ContratoClient(builder.build()).crear(UUID.randomUUID(), "  ", ENTREGA);

        servidor.verify();
    }

    @Test
    void conElClienteHttpRealSoloLlegaElCorreoDelEmpleado() throws Exception {
        AtomicReference<List<String>> recibido = new AtomicReference<>();
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", exchange -> {
            recibido.set(exchange.getRequestHeaders().get("X-Usuario"));
            exchange.sendResponseHeaders(201, -1);
            exchange.close();
        });
        servidor.start();
        try {
            RestClient real = RestClient.builder()
                    .baseUrl("http://127.0.0.1:" + servidor.getAddress().getPort())
                    .requestFactory(new JdkClientHttpRequestFactory(
                            HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()))
                    .defaultHeader("X-Rol", "SERVICIO")
                    .defaultHeader("X-Usuario", "service-reserva")
                    .build();

            new ContratoClient(real).crear(UUID.randomUUID(), "agente@hertz.test", ENTREGA);

            assertEquals(List.of("agente@hertz.test"), recibido.get());
        } finally {
            servidor.stop(0);
        }
    }
}
