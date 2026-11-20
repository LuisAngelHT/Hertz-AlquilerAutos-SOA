package com.hertz.reserva.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** Reloj en hora de Lima: "hoy" para validar fechas es el día de Perú, no el del servidor. En las pruebas se reemplaza por uno fijo. */
@Configuration
public class TiempoConfig {

    @Bean
    public Clock reloj() {
        return Clock.system(ZoneId.of("America/Lima"));
    }
}
