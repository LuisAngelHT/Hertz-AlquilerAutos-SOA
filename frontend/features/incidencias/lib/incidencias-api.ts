import { ApiRequestError, requestJson } from "@/lib/api-client";
import type {
  CierreIncidencia,
  EstadoIncidencia,
  FichaTaller,
  GuardarFicha,
  Incidencia,
  NuevaIncidencia,
  PaginaIncidencias,
  RegistroIncidencia,
} from "@/features/incidencias/types/incidencia";

function authHeaders(token: string): HeadersInit {
  return { Authorization: `Bearer ${token}` };
}

export function mensajeErrorIncidencia(cause: unknown): string {
  if (!(cause instanceof ApiRequestError)) {
    return cause instanceof Error
      ? cause.message
      : "No se pudo completar la operación. Inténtalo nuevamente.";
  }

  switch (cause.code) {
    case "VEHICULO_NO_ENCONTRADO":
      return "No se encontró un vehículo con esa placa.";
    case "SUCURSAL_NO_AUTORIZADA":
      return "Solo puedes registrar o consultar incidencias de vehículos de tu sucursal.";
    case "INCIDENCIA_NO_ENCONTRADA":
      return "No se encontró la incidencia solicitada.";
    case "INCIDENCIA_CERRADA":
      return "La incidencia ya está cerrada: su ficha solo se puede consultar.";
    case "INCIDENCIA_INVALIDA":
      return cause.message || "Revisa los datos de la ficha.";
    case "VEHICULO_ALQUILADO":
      return (
        cause.message ||
        "El vehículo está alquilado. Registra primero su devolución en Reservas y después la incidencia."
      );
    case "SERVICIO_NO_DISPONIBLE":
      return cause.message || "Un servicio no está disponible. Inténtalo de nuevo.";
    case "SOLICITUD_INVALIDA":
    case "TRANSICION_INVALIDA":
      return cause.message || "Revisa los datos ingresados.";
    default:
      return cause.message;
  }
}

export function listarIncidencias(
  token: string,
  filtro: {
    idVehiculo?: string;
    estado?: EstadoIncidencia;
    pagina: number;
    tamano: number;
  },
): Promise<PaginaIncidencias> {
  const params = new URLSearchParams({
    pagina: String(filtro.pagina),
    tamano: String(filtro.tamano),
  });
  if (filtro.idVehiculo?.trim()) {
    params.set("idVehiculo", filtro.idVehiculo.trim().toUpperCase());
  }
  if (filtro.estado) params.set("estado", filtro.estado);

  return requestJson(`/api/v1/incidencia?${params.toString()}`, {
    headers: authHeaders(token),
  });
}

/**
 * PUT /api/v1/incidencia/{id}/cerrar — la cierra y, si era la última abierta, el vehículo vuelve a DISPONIBLE.
 * Con {trabajoRealizado, costoFinal} deja constancia de lo que se reparó en la ficha del taller.
 */
export function cerrarIncidencia(
  token: string,
  idIncidencia: string,
  cierre?: CierreIncidencia,
): Promise<Incidencia> {
  return requestJson(`/api/v1/incidencia/${encodeURIComponent(idIncidencia)}/cerrar`, {
    method: "PUT",
    headers: authHeaders(token),
    ...(cierre ? { body: JSON.stringify(cierre) } : {}),
  });
}

/** GET /api/v1/incidencia/{id}/ficha — la ficha del taller (vacía si todavía no se llenó). */
export function obtenerFicha(token: string, idIncidencia: string): Promise<FichaTaller> {
  return requestJson(`/api/v1/incidencia/${encodeURIComponent(idIncidencia)}/ficha`, {
    headers: authHeaders(token),
  });
}

/** PUT /api/v1/incidencia/{id}/ficha — guarda diagnóstico, plan, costos y las listas de daños completas. */
export function guardarFicha(token: string, idIncidencia: string, ficha: GuardarFicha): Promise<FichaTaller> {
  return requestJson(`/api/v1/incidencia/${encodeURIComponent(idIncidencia)}/ficha`, {
    method: "PUT",
    headers: authHeaders(token),
    body: JSON.stringify(ficha),
  });
}

/** POST /api/v1/incidencia/{id}/ficha/seguimiento — agrega una nota de avance. */
export function agregarSeguimiento(token: string, idIncidencia: string, nota: string): Promise<FichaTaller> {
  return requestJson(`/api/v1/incidencia/${encodeURIComponent(idIncidencia)}/ficha/seguimiento`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify({ nota }),
  });
}

export function registrarIncidencia(
  token: string,
  request: NuevaIncidencia,
): Promise<RegistroIncidencia> {
  return requestJson("/api/v1/incidencia", {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify(request),
  });
}
