package com.hertz.reserva.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Clientes HTTP hacia ServicioFlota, ServicioCliente y ServicioContrato. Llaman directo al servicio (no por el
 * gateway) con el secreto compartido y el rol interno SERVICIO. Los tiempos de espera son
 * generosos porque en Render un servicio dormido tarda en arrancar; la reserva nunca queda
 * colgada: pasado el límite responde 503 SERVICIO_NO_DISPONIBLE.
 */
@Configuration
public class RestClientsConfig {

    private static RestClient crear(String baseUrl, String secreto) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(40));
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("X-Gateway-Secret", secreto)
                .defaultHeader("X-Rol", "SERVICIO")
                .defaultHeader("X-Usuario", "service-reserva")
                .build();
    }

    @Bean
    public RestClient flotaRestClient(@Value("${services.flota.url}") String url,
                                      @Value("${gateway.secret}") String secreto) {
        return crear(url, secreto);
    }

    @Bean
    public RestClient clienteRestClient(@Value("${services.cliente.url}") String url,
                                        @Value("${gateway.secret}") String secreto) {
        return crear(url, secreto);
    }

    @Bean
    public RestClient contratoRestClient(@Value("${services.contrato.url}") String url,
                                         @Value("${gateway.secret}") String secreto) {
        return crear(url, secreto);
    }
}
