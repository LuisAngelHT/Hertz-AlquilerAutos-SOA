package com.hertz.reserva;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SVC-02 ServicioReserva (Tarea, dominio Reservas).
 *
 * Gestiona el ciclo de vida de una reserva: crear, modificar, cancelar y consultar. Para crear o
 * reprogramar valida al cliente con ServicioCliente (SVC-01) y aparta un vehículo con
 * ServicioFlota (SVC-03), que es el único dueño de la disponibilidad.
 *
 * Datos: esquema "reserva" de Supabase (PostgreSQL), con el rol propio "svc_reserva".
 * Corre en el puerto 8084 y solo se debe llamar a través del API Gateway.
 */
@SpringBootApplication
public class ServiceReservaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceReservaApplication.class, args);
    }
}
