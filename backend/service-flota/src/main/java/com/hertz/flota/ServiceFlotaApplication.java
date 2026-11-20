package com.hertz.flota;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SVC-03 ServicioFlota (Entidad, dominio Flota).
 *
 * Controla el inventario de vehículos: consulta la disponibilidad por sucursal, categoría y
 * fechas, consulta un vehículo por su placa y cambia su estado (DISPONIBLE, ALQUILADO o
 * MANTENIMIENTO). Es el único servicio que escribe el estado de un vehículo: los demás
 * (reservas, contratos, incidencias) se lo piden a él.
 *
 * Datos: esquema "flota" de Supabase (PostgreSQL), con el rol propio "svc_flota".
 * Corre en el puerto 8083 y solo se debe llamar a través del API Gateway.
 */
@SpringBootApplication
public class ServiceFlotaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceFlotaApplication.class, args);
    }
}
