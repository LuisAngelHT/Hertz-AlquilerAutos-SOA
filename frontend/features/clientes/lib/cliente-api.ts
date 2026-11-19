import { requestJson } from "@/lib/api-client";
import type {
  Cliente,
  ClienteActualizacionRequest,
  ClienteRegistroRequest,
  TipoDocumento,
} from "@/features/clientes/types/cliente";

function authHeaders(token: string): HeadersInit {
  return {
    Authorization: `Bearer ${token}`,
  };
}

export function buscarCliente(
  token: string,
  tipoDocumento: TipoDocumento,
  numeroDocumento: string,
): Promise<Cliente> {
  const query = new URLSearchParams({
    tipoDocumento,
    numeroDocumento: numeroDocumento.trim(),
  });

  return requestJson(
    `/api/v1/cliente?${query.toString()}`,
    { headers: authHeaders(token) },
  );
}

type ModalidadRegistro =
  | { modalidad: "autoregistro" }
  | { modalidad: "presencial"; token: string };

export function registrarCliente(
  request: ClienteRegistroRequest,
  opciones: ModalidadRegistro,
): Promise<Cliente> {
  const esAutoregistro = opciones.modalidad === "autoregistro";
  const headers = esAutoregistro
    ? undefined
    : authHeaders(opciones.token);

  return requestJson(
    esAutoregistro ? "/api/v1/cliente/registro" : "/api/v1/cliente",
    {
      method: "POST",
      headers,
      body: JSON.stringify(request),
    },
  );
}

export function actualizarCliente(
  token: string,
  idCliente: string,
  request: ClienteActualizacionRequest,
): Promise<Cliente> {
  return requestJson(`/api/v1/cliente/${encodeURIComponent(idCliente)}`, {
    method: "PUT",
    headers: authHeaders(token),
    body: JSON.stringify(request),
  });
}
