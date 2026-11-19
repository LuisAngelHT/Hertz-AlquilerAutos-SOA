package com.hertz.reserva.exception;

import org.springframework.http.HttpStatus;

/**
 * Error de negocio del servicio de reservas. Lleva el código del error (los que define el
 * documento y algunos más) y el estado HTTP con el que se responde.
 */
public class ReservaException extends RuntimeException {

    private final String codigo;
    private final HttpStatus status;

    public ReservaException(String codigo, HttpStatus status, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
        this.status = status;
    }

    public String getCodigo() { return codigo; }
    public HttpStatus getStatus() { return status; }

    // 404 (documento): la reserva no existe, o no es del cliente que consulta (no se revela cuál).
    public static ReservaException noEncontrada() {
        return new ReservaException("RESERVA_NO_ENCONTRADA", HttpStatus.NOT_FOUND, "La reserva no existe");
    }

    // 409 (documento): no hay vehículos de esa categoría en esas fechas.
    public static ReservaException sinDisponibilidad(String detalle) {
        return new ReservaException("SIN_DISPONIBILIDAD", HttpStatus.CONFLICT, detalle);
    }

    // 409: el vehículo concreto que eligió el cliente ya se reservó o cambió de estado.
    public static ReservaException vehiculoElegidoNoDisponible() {
        return new ReservaException("VEHICULO_ELEGIDO_NO_DISPONIBLE", HttpStatus.CONFLICT,
                "El vehiculo que elegiste ya no esta disponible en esas fechas; elige otro o reserva por categoria");
    }

    // 404: el cliente no existe en ServicioCliente (precondición 1 del documento).
    public static ReservaException clienteNoEncontrado() {
        return new ReservaException("CLIENTE_NO_ENCONTRADO", HttpStatus.NOT_FOUND,
                "El cliente no existe en ServicioCliente");
    }

    public static ReservaException solicitudInvalida(String detalle) {
        return new ReservaException("SOLICITUD_INVALIDA", HttpStatus.BAD_REQUEST, detalle);
    }

    public static ReservaException rolNoAutorizado(String detalle) {
        return new ReservaException("ROL_NO_AUTORIZADO", HttpStatus.FORBIDDEN, detalle);
    }

    public static ReservaException sucursalNoAutorizada(String sucursalReserva) {
        return new ReservaException("SUCURSAL_NO_AUTORIZADA", HttpStatus.FORBIDDEN,
                "Solo puedes gestionar reservas con recojo en tu sucursal; esta es de " + sucursalReserva);
    }

    // 409: la acción no es válida para el estado actual (por ejemplo, cancelar una reserva EN_CURSO).
    public static ReservaException transicionInvalida(String detalle) {
        return new ReservaException("TRANSICION_INVALIDA", HttpStatus.CONFLICT, detalle);
    }

    // 409: otra persona modificó la misma reserva al mismo tiempo.
    public static ReservaException conflictoConcurrencia() {
        return new ReservaException("CONFLICTO_CONCURRENCIA", HttpStatus.CONFLICT,
                "La reserva cambió mientras la editabas; vuelve a consultarla e intenta otra vez");
    }

    // 409: el cliente ya tiene el máximo de reservas activas.
    public static ReservaException limiteReservas(int maximo) {
        return new ReservaException("LIMITE_RESERVAS", HttpStatus.CONFLICT,
                "El cliente ya tiene " + maximo + " reservas activas; cancela o finaliza una antes de reservar otra");
    }

    // 409: sin licencia registrada o vencida antes de terminar el alquiler.
    public static ReservaException licenciaNoVigente(String detalle) {
        return new ReservaException("LICENCIA_NO_VIGENTE", HttpStatus.CONFLICT, detalle);
    }

    // 503: no se pudo hablar con otro servicio (flota o cliente).
    public static ReservaException servicioNoDisponible(String servicio, String detalle) {
        return new ReservaException("SERVICIO_NO_DISPONIBLE", HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo comunicar con " + servicio + ": " + detalle);
    }
}
