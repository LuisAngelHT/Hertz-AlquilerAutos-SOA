import { ApiRequestError, requestJson } from "@/lib/api-client";
import type {
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
    cache: "no-store",
    headers: { Authorization: `Bearer ${token}` },
  });
}
