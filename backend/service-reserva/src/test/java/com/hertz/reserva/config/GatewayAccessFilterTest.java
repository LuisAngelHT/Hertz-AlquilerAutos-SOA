package com.hertz.reserva.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas del filtro que solo deja pasar solicitudes del API Gateway. */
class GatewayAccessFilterTest {

    private final GatewayAccessFilter filtro = new GatewayAccessFilter("secreto-de-prueba");

    private MockHttpServletResponse llamar(String uri, String secreto, MockFilterChain cadena) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRequestURI(uri);
        if (secreto != null) {
            request.addHeader("X-Gateway-Secret", secreto);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filtro.doFilter(request, response, cadena);
        return response;
    }

    @Test
    void conSecretoCorrectoPasa() throws Exception {
        MockFilterChain cadena = new MockFilterChain();
        MockHttpServletResponse r = llamar("/api/v1/flota/vehiculos", "secreto-de-prueba", cadena);
        assertEquals(200, r.getStatus());
        assertNotNull(cadena.getRequest());
    }

    @Test
    void sinSecretoSeRechaza() throws Exception {
        MockFilterChain cadena = new MockFilterChain();
        MockHttpServletResponse r = llamar("/api/v1/flota/vehiculos", null, cadena);
        assertEquals(403, r.getStatus());
        assertTrue(r.getContentAsString().contains("ACCESO_NO_AUTORIZADO"));
        assertNull(cadena.getRequest());
    }

    @Test
    void conSecretoIncorrectoSeRechaza() throws Exception {
        MockFilterChain cadena = new MockFilterChain();
        MockHttpServletResponse r = llamar("/api/v1/flota/vehiculos", "otro", cadena);
        assertEquals(403, r.getStatus());
        assertNull(cadena.getRequest());
    }

    @Test
    void elHealthCheckPasaSinSecreto() throws Exception {
        MockFilterChain cadena = new MockFilterChain();
        MockHttpServletResponse r = llamar("/actuator/health", null, cadena);
        assertEquals(200, r.getStatus());
        assertNotNull(cadena.getRequest());
    }
}
