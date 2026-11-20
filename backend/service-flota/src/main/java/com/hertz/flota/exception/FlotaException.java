package com.hertz.flota.exception;

import org.springframework.http.HttpStatus;

/**
 * Error de negocio del servicio de flota. Lleva el código del error (el que define el
 * documento) y el estado HTTP con el que se responde. GlobalExceptionHandler la convierte
 * en un ErrorResponse JSON.
 */
public class FlotaException extends RuntimeException {

    private final String codigo;
    private final HttpStatus status;

    public FlotaException(String codigo, HttpStatus status, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
        this.status = status;
    }

    public String getCodigo() { return codigo; }
    public HttpStatus getStatus() { return status; }

    // 404: la placa no existe en la flota.
    public static FlotaException vehiculoNoEncontrado(String placa) {
        return new FlotaException("VEHICULO_NO_ENCONTRADO", HttpStatus.NOT_FOUND,
                "No existe un vehiculo con placa " + placa);
    }

    // 409: se quiso alquilar un vehículo que no está DISPONIBLE (ya alquilado o en mantenimiento).
    public static FlotaException vehiculoNoDisponible(String placa) {
        return new FlotaException("VEHICULO_NO_DISPONIBLE", HttpStatus.CONFLICT,
                "El vehiculo " + placa + " no esta en estado DISPONIBLE");
    }

    // 403: el rol del usuario no puede cambiar el estado (solo AGENTE y ADMINISTRADOR).
    public static FlotaException rolNoAutorizado() {
        return new FlotaException("ROL_NO_AUTORIZADO", HttpStatus.FORBIDDEN,
                "Solo los roles AGENTE, ADMINISTRADOR o ADMINISTRADOR_SUCURSAL pueden cambiar el estado de un vehiculo");
    }

    // 403: el usuario es de una sucursal (o no tiene sucursal asignada) y el vehículo es de otra.
    public static FlotaException sucursalNoAutorizada(String placa, String sucursalVehiculo) {
        return new FlotaException("SUCURSAL_NO_AUTORIZADA", HttpStatus.FORBIDDEN,
                "Solo puedes cambiar el estado de vehiculos de tu sucursal; el vehiculo " + placa
                        + " es de " + sucursalVehiculo);
    }

    // 400: datos mal enviados (por ejemplo fechaFin anterior a fechaInicio).
    public static FlotaException solicitudInvalida(String detalle) {
        return new FlotaException("SOLICITUD_INVALIDA", HttpStatus.BAD_REQUEST, detalle);
    }

    // 403: alta y baja de vehiculos (y reactivarlos) solo las hace el ADMINISTRADOR general.
    public static FlotaException soloAdministradorGeneral() {
        return new FlotaException("ROL_NO_AUTORIZADO", HttpStatus.FORBIDDEN,
                "Solo el ADMINISTRADOR general puede registrar, dar de baja o reactivar vehiculos");
    }

    // 409: ya existe un vehiculo con esa placa.
    public static FlotaException placaDuplicada(String placa) {
        return new FlotaException("PLACA_DUPLICADA", HttpStatus.CONFLICT,
                "Ya existe un vehiculo con placa " + placa);
    }

    // 409: no se da de baja un vehiculo que esta con un cliente.
    public static FlotaException vehiculoAlquilado(String placa) {
        return new FlotaException("VEHICULO_ALQUILADO", HttpStatus.CONFLICT,
                "El vehiculo " + placa + " esta alquilado; registra primero su devolucion para darlo de baja");
    }

    // 409: tiene reservas o contratos pendientes; hay que cancelarlos o reasignarlos antes de la baja.
    public static FlotaException vehiculoConReservas(String placa, long cantidad) {
        return new FlotaException("VEHICULO_CON_RESERVAS", HttpStatus.CONFLICT,
                "El vehiculo " + placa + " tiene " + cantidad + (cantidad == 1 ? " reserva pendiente" : " reservas pendientes")
                        + "; cancelalas o reasignalas antes de darlo de baja");
    }

    // 409: un vehiculo fuera de servicio solo puede reactivarse.
    public static FlotaException transicionInvalida(String detalle) {
        return new FlotaException("TRANSICION_INVALIDA", HttpStatus.CONFLICT, detalle);
    }

    // 409: no hay vehiculos de esa categoria libres en esas fechas.
    public static FlotaException sinDisponibilidad(String categoria, String sucursal) {
        return new FlotaException("SIN_DISPONIBILIDAD", HttpStatus.CONFLICT,
                "No hay vehiculos de categoria " + categoria + " disponibles en " + sucursal + " en esas fechas");
    }

    // 409: el vehiculo concreto que eligio el cliente ya no esta libre en esas fechas.
    public static FlotaException vehiculoElegidoNoDisponible(String placa) {
        return new FlotaException("VEHICULO_NO_DISPONIBLE", HttpStatus.CONFLICT,
                "El vehiculo " + placa + " ya no esta disponible en esas fechas y sucursal");
    }

    // 404: no existe un bloqueo para esa referencia.
    public static FlotaException bloqueoNoEncontrado(String referencia) {
        return new FlotaException("BLOQUEO_NO_ENCONTRADO", HttpStatus.NOT_FOUND,
                "No hay un vehiculo apartado para la referencia " + referencia);
    }

    // 403: solo ServicioReserva (rol interno SERVICIO) puede apartar o liberar vehiculos.
    public static FlotaException soloServicios() {
        return new FlotaException("ROL_NO_AUTORIZADO", HttpStatus.FORBIDDEN,
                "Solo un servicio interno puede apartar o liberar vehiculos");
    }
}
