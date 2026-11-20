import { ApiRequestError, requestJson } from "@/lib/api-client";
import type {
  EstadoIncidencia,
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

/** PUT /api/v1/incidencia/{id}/cerrar — la cierra y, si era la última abierta, el vehículo vuelve a DISPONIBLE. */
export function cerrarIncidencia(token: string, idIncidencia: string): Promise<Incidencia> {
  return requestJson(`/api/v1/incidencia/${encodeURIComponent(idIncidencia)}/cerrar`, {
    method: "PUT",
    headers: authHeaders(token),
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
