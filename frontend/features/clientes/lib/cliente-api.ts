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

/** Una página del listado de todos los clientes (solo personal). */
export interface PaginaClientes {
  total: number;
  pagina: number; // empieza en 0
  tamano: number;
  totalPaginas: number;
  clientes: Cliente[];
}

/** GET /api/v1/cliente/todos — todos los clientes, paginado, con búsqueda opcional por nombre, documento o correo. */
export function listarClientes(
  token: string,
  filtro: { texto?: string; pagina?: number; tamano?: number } = {},
): Promise<PaginaClientes> {
  const query = new URLSearchParams();
  if (filtro.texto?.trim()) query.set("texto", filtro.texto.trim());
  if (filtro.pagina !== undefined) query.set("pagina", String(filtro.pagina));
  if (filtro.tamano !== undefined) query.set("tamano", String(filtro.tamano));
  const sufijo = query.toString();
  return requestJson(`/api/v1/cliente/todos${sufijo ? `?${sufijo}` : ""}`, {
    headers: authHeaders(token),
  });
}

/** GET /api/v1/cliente/yo — el perfil del cliente que inició sesión. */
export function obtenerMiPerfil(token: string): Promise<Cliente> {
  return requestJson("/api/v1/cliente/yo", { headers: authHeaders(token) });
}

/** PUT /api/v1/cliente/yo — el cliente actualiza su teléfono y su licencia. */
export function actualizarMiPerfil(
  token: string,
  datos: { telefono: string; licenciaConducir: string; vencimientoLicencia: string },
): Promise<Cliente> {
  return requestJson("/api/v1/cliente/yo", {
    method: "PUT",
    headers: authHeaders(token),
    body: JSON.stringify(datos),
  });
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
