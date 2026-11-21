import { API_BASE_URL, ApiRequestError, requestJson } from "@/lib/api-client";
import type {
  Contrato,
  FiltroContratos,
  PaginaContratos,
} from "@/features/contratos/types/contrato";

export function mensajeErrorContrato(cause: unknown): string {
  if (!(cause instanceof ApiRequestError)) {
    return cause instanceof TypeError
      ? "No se pudo conectar con el servidor. Revisa que el API Gateway esté disponible."
      : cause instanceof Error
        ? cause.message
        : "No se pudo cargar la información. Inténtalo nuevamente.";
  }

  switch (cause.code) {
    case "ROL_NO_AUTORIZADO":
      return "Tu cuenta no tiene acceso al módulo de contratos.";
    case "SUCURSAL_NO_AUTORIZADA":
      return "No tienes acceso a contratos de esta sucursal.";
    case "CONTRATO_NO_ENCONTRADO":
      return "No se encontró el contrato solicitado.";
    case "SERVICIO_NO_DISPONIBLE":
      return "El servicio de contratos no está disponible. Inténtalo de nuevo en unos segundos.";
    case "REVISION_INVALIDA":
      return cause.message || "Revisa los datos de la revisión del vehículo.";
    case "CONTRATO_NO_FIRMADO":
      return "Confirma que el contrato impreso fue firmado por el cliente.";
    case "DOCUMENTO_INVALIDO":
      return cause.message || "El archivo no es válido. Sube un PDF, JPG o PNG.";
    case "DOCUMENTO_MUY_GRANDE":
      return "El archivo supera el máximo permitido de 5 MB.";
    case "DOCUMENTO_NO_ENCONTRADO":
      return "Este contrato todavía no tiene el contrato firmado escaneado.";
    case "SOLICITUD_INVALIDA":
      return cause.message || "Revisa los filtros ingresados.";
    default:
      return cause.message || "No se pudo completar la consulta.";
  }
}

export function listarContratos(
  token: string,
  filtro: FiltroContratos,
): Promise<PaginaContratos> {
  const params = new URLSearchParams({
    pagina: String(filtro.pagina),
    tamano: String(filtro.tamano),
  });

  if (filtro.placa?.trim()) params.set("placa", filtro.placa.trim().toUpperCase());
  if (filtro.idReserva?.trim()) params.set("idReserva", filtro.idReserva.trim());
  if (filtro.idCliente?.trim()) params.set("idCliente", filtro.idCliente.trim());
  if (filtro.estado) params.set("estado", filtro.estado);

  return requestJson(`/api/v1/contrato?${params.toString()}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
}

async function errorDeRespuesta(response: Response): Promise<ApiRequestError> {
  let mensaje: string | undefined;
  let codigo: string | undefined;
  try {
    const payload: unknown = await response.json();
    if (typeof payload === "object" && payload !== null) {
      const dato = payload as Record<string, unknown>;
      if (typeof dato.mensaje === "string") mensaje = dato.mensaje;
      if (typeof dato.codigo === "string") codigo = dato.codigo;
    }
  } catch {
    /* respuesta sin cuerpo legible */
  }
  return new ApiRequestError(
    mensaje ?? `La solicitud falló con HTTP ${response.status}.`,
    response.status,
    codigo,
  );
}

/** Sube (o reemplaza) el contrato firmado y escaneado. Devuelve el contrato ya con sus datos. */
export async function subirDocumentoContrato(
  token: string,
  idContrato: string,
  archivo: File,
): Promise<Contrato> {
  const formulario = new FormData();
  formulario.append("archivo", archivo);
  // Sin Content-Type: el navegador lo completa con el separador del formulario.
  const response = await fetch(`${API_BASE_URL}/api/v1/contrato/${idContrato}/documento`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    body: formulario,
    credentials: "omit",
  });
  if (!response.ok) throw await errorDeRespuesta(response);
  return (await response.json()) as Contrato;
}

/** Descarga el contrato firmado para abrirlo (la peticion lleva el token, por eso no es un enlace directo). */
export async function descargarDocumentoContrato(token: string, idContrato: string): Promise<Blob> {
  const response = await fetch(`${API_BASE_URL}/api/v1/contrato/${idContrato}/documento`, {
    headers: { Authorization: `Bearer ${token}` },
    credentials: "omit",
  });
  if (!response.ok) throw await errorDeRespuesta(response);
  return response.blob();
}

/** El contrato de una reserva (con las revisiones de entrega y devolución). 404 si todavía no se entregó. */
export function obtenerContratoPorReserva(token: string, idReserva: string): Promise<Contrato> {
  return requestJson(`/api/v1/contrato/reserva/${encodeURIComponent(idReserva)}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
}

/** Un contrato por su id (con las revisiones de entrega y devolución). */
export function obtenerContrato(token: string, idContrato: string): Promise<Contrato> {
  return requestJson(`/api/v1/contrato/${encodeURIComponent(idContrato)}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
}
