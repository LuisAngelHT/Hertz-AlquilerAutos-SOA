import type { Sesion } from "@/features/flota/lib/session";

/**
 * Llamadas al servicio de reservas (SVC-02) a través del API Gateway.
 * Cada llamada lleva el token del login en el header Authorization.
 */

export type EstadoReserva = "CONFIRMADA" | "EN_CURSO" | "FINALIZADA" | "CANCELADA";

export interface Reserva {
  idReserva: string;
  estado: EstadoReserva;
  idCliente: string;
  categoria: string;
  sucursalRecojo: string;
  sucursalDevolucion: string;
  fechaInicio: string; // AAAA-MM-DD
  fechaFin: string; // AAAA-MM-DD
  placaAsignada: string | null;
  /** Quién alquiló (foto de los datos al reservar); null en reservas antiguas. */
  clienteNombre: string | null;
  clienteDocumento: string | null;
  clienteTelefono: string | null;
  creadoEn: string; // ISO 8601
}

/** Una página del listado: "total" cuenta todas las reservas que cumplen el filtro. */
export interface PaginaReservas {
  total: number;
  pagina: number; // empieza en 0
  tamano: number;
  totalPaginas: number;
  reservas: Reserva[];
}

/** Un cambio del historial de una reserva: quién lo hizo, cuándo y qué cambió. */
export interface CambioReserva {
  accion: string;
  estadoAnterior: EstadoReserva | null;
  estadoNuevo: EstadoReserva;
  usuario: string;
  rol: string;
  detalle: string | null;
  fecha: string; // ISO 8601
}

export interface NuevaReserva {
  /** Solo lo envía el personal (a qué cliente se reserva). El CLIENTE usa su propio perfil. */
  idCliente?: string;
  categoria: string;
  sucursalRecojo: string;
  sucursalDevolucion: string;
  fechaInicio: string;
  fechaFin: string;
  /** Opcional: el vehículo concreto que eligió el cliente en el catálogo. Sin placa se asigna uno de la categoría. */
  placa?: string;
}

export interface CambiosReserva {
  categoria?: string;
  fechaInicio?: string;
  fechaFin?: string;
  estado?: EstadoReserva;
  motivo?: string;
}

export interface FiltroReservas {
  tipoDocumento?: string;
  numeroDocumento?: string;
  estado?: EstadoReserva;
  sucursal?: string; // sucursal de recojo; vacío = todas
  recogenHoy?: boolean; // solo reservas confirmadas que empiezan hoy
  pagina?: number;
  tamano?: number;
}

/** El token venció o no es válido: hay que volver a iniciar sesión. */
export class SesionExpiradaError extends Error {
  constructor() {
    super("Tu sesión venció. Inicia sesión de nuevo.");
    this.name = "SesionExpiradaError";
  }
}

const API_BASE_URL = (
  process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080"
).replace(/\/+$/, "");

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

// Traduce los códigos de error del backend (los del documento y algunos más) a mensajes para el usuario.
function mensajeDeError(codigo: string | undefined, mensaje: string | undefined): string {
  switch (codigo) {
    case "VEHICULO_ELEGIDO_NO_DISPONIBLE":
      return "El vehículo que elegiste ya no está disponible en esas fechas. Vuelve al catálogo para elegir otro, o reserva solo por categoría.";
    case "SIN_DISPONIBILIDAD":
      return "No hay vehículos de esa categoría disponibles en esas fechas y sucursal. Prueba con otras fechas u otra categoría.";
    case "RESERVA_NO_ENCONTRADA":
      return "La reserva no existe.";
    case "CLIENTE_NO_ENCONTRADO":
      return "No se encontró el cliente. Regístralo primero en la sección Clientes.";
    case "ROL_NO_AUTORIZADO":
      return mensaje ?? "Tu rol no puede realizar esta acción.";
    case "SUCURSAL_NO_AUTORIZADA":
      return "Solo puedes gestionar reservas con recojo en tu sucursal.";
    case "TRANSICION_INVALIDA":
      return mensaje ?? "Esa acción no es válida para el estado actual de la reserva.";
    case "CONFLICTO_CONCURRENCIA":
      return "Otra persona modificó esta reserva al mismo tiempo. Actualiza la lista e inténtalo de nuevo.";
    case "LIMITE_RESERVAS":
      return mensaje ?? "El cliente ya tiene el máximo de reservas activas.";
    case "LICENCIA_NO_VIGENTE":
      return mensaje ?? "La licencia del cliente no está vigente para esas fechas.";
    case "SERVICIO_NO_DISPONIBLE":
      return mensaje ?? "Un servicio no respondió. Inténtalo de nuevo en unos segundos.";
    case "SOLICITUD_INVALIDA":
      return mensaje ?? "Revisa los datos ingresados.";
    default:
      return mensaje ?? "No se pudo completar la operación. Inténtalo de nuevo.";
  }
}

async function solicitar<T>(
  sesion: Sesion,
  ruta: string,
  init: { method?: string; body?: unknown } = {},
): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}/api/v1/reserva${ruta}`, {
      method: init.method ?? "GET",
      headers: {
        Authorization: `${sesion.tipo || "Bearer"} ${sesion.token}`,
        ...(init.body !== undefined ? { "Content-Type": "application/json" } : {}),
      },
      body: init.body !== undefined ? JSON.stringify(init.body) : undefined,
      credentials: "omit",
    });
  } catch {
    throw new Error("No se pudo conectar con el servidor. Revisa que el gateway esté encendido.");
  }

  if (response.status === 401) {
    throw new SesionExpiradaError();
  }

  const texto = await response.text();
  let datos: unknown = null;
  try {
    datos = texto ? JSON.parse(texto) : null;
  } catch {
    datos = null;
  }

  if (!response.ok) {
    const err = isRecord(datos) ? datos : {};
    const codigo = typeof err.codigo === "string" ? err.codigo : undefined;
    // 5xx sin código del negocio = el servicio está caído; con código (503 SERVICIO_NO_DISPONIBLE) se muestra su mensaje.
    if (response.status >= 500 && !codigo) {
      throw new Error(
        `El servidor respondió con HTTP ${response.status}. Revisa que el servicio de reservas esté disponible.`,
      );
    }
    throw new Error(
      mensajeDeError(codigo, typeof err.mensaje === "string" ? err.mensaje : undefined),
    );
  }

  if (datos === null) {
    throw new Error("El servidor devolvió una respuesta que no se pudo leer.");
  }
  return datos as T;
}

/** POST /api/v1/reserva — crea la reserva CONFIRMADA y aparta un vehículo en flota. */
export function crearReserva(sesion: Sesion, datos: NuevaReserva): Promise<Reserva> {
  return solicitar<Reserva>(sesion, "", { method: "POST", body: datos });
}

/** GET /api/v1/reserva — una página de reservas. El CLIENTE solo recibe las suyas. */
export function listarReservas(sesion: Sesion, filtro: FiltroReservas = {}): Promise<PaginaReservas> {
  const params = new URLSearchParams();
  if (filtro.tipoDocumento && filtro.numeroDocumento?.trim()) {
    params.set("tipoDocumento", filtro.tipoDocumento);
    params.set("numeroDocumento", filtro.numeroDocumento.trim());
  }
  if (filtro.estado) params.set("estado", filtro.estado);
  if (filtro.sucursal) params.set("sucursal", filtro.sucursal);
  if (filtro.recogenHoy) params.set("recogenHoy", "true");
  if (filtro.pagina !== undefined) params.set("pagina", String(filtro.pagina));
  if (filtro.tamano !== undefined) params.set("tamano", String(filtro.tamano));
  const query = params.toString();
  return solicitar<PaginaReservas>(sesion, query ? `?${query}` : "");
}

/** PUT /api/v1/reserva/{id} — cambia fechas/categoría (solo CONFIRMADA) o el estado (check-out, devolución). */
export function modificarReserva(
  sesion: Sesion,
  idReserva: string,
  cambios: CambiosReserva,
): Promise<Reserva> {
  return solicitar<Reserva>(sesion, `/${encodeURIComponent(idReserva)}`, {
    method: "PUT",
    body: cambios,
  });
}

/** POST /api/v1/reserva/{id}/cancelar — la deja CANCELADA y libera el vehículo. */
export function cancelarReserva(
  sesion: Sesion,
  idReserva: string,
  motivo?: string,
): Promise<Reserva> {
  const texto = motivo?.trim();
  return solicitar<Reserva>(sesion, `/${encodeURIComponent(idReserva)}/cancelar`, {
    method: "POST",
    body: texto ? { motivo: texto } : {},
  });
}

/** GET /api/v1/reserva/{id}/historial — solo personal; del cambio más reciente al más antiguo. */
export function consultarHistorialReserva(
  sesion: Sesion,
  idReserva: string,
): Promise<CambioReserva[]> {
  return solicitar<CambioReserva[]>(sesion, `/${encodeURIComponent(idReserva)}/historial`);
}
