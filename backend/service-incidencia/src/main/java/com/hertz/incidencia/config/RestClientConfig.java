package com.hertz.incidencia.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient flotaRestClient(@Value("${services.flota.url}") String baseUrl,
                                      @Value("${gateway.secret}") String secreto) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(40));   // margen para un arranque en frío

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("X-Gateway-Secret", secreto)
                .requestInterceptor(this::propagarIdentidad)
                .build();
    }

    // Flota recibe la identidad del usuario que registra la incidencia y aplica sus propias reglas
    private ClientHttpResponse propagarIdentidad(HttpRequest request, byte[] cuerpo,
                                                 ClientHttpRequestExecution ejecucion) throws IOException {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            for (String cabecera : List.of("X-Rol", "X-Usuario", "X-Sucursal")) {
                String valor = atributos.getRequest().getHeader(cabecera);
                if (valor != null && !valor.isBlank()) {
                    request.getHeaders().set(cabecera, valor);
                }
            }
        }
        return ejecucion.execute(request, cuerpo);
    }
}