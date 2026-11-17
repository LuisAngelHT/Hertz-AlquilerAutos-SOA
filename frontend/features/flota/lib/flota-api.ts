import type { Sesion } from "./session";

/**
 * Llamadas al servicio de flota (SVC-03) a través del API Gateway.
 * Cada llamada lleva el token del login en el header Authorization.
 */

export type EstadoVehiculo = "DISPONIBLE" | "ALQUILADO" | "MANTENIMIENTO";

export interface Vehiculo {
  idVehiculo: string;
  placa: string;
  categoria: string;
  marca: string;
  modelo: string;
  anio: number;
  estado: EstadoVehiculo;
  sucursal: string;
}

export interface Disponibilidad {
  categoria: string;
  cantidadDisponible: number;
  vehiculos: Vehiculo[];
}

/** Un cambio de estado del historial: quién lo hizo, cuándo y por qué. */
export interface CambioHistorial {
  placa: string;
  estadoAnterior: EstadoVehiculo;
  estadoNuevo: EstadoVehiculo;
  usuario: string;
  rol: string;
  motivo: string | null;
  fecha: string; // ISO 8601
}

export interface FiltroListado {
  estado?: EstadoVehiculo;
  sucursal?: string; // vacío = todas
  texto?: string; // marca o modelo
  pagina?: number; // empieza en 0
  tamano?: number; // vehículos por página (el backend usa 12 y acepta hasta 50)
}

/** Una página del listado: "total" cuenta todos los vehículos que cumplen el filtro. */
export interface PaginaVehiculos {
  total: number;
  pagina: number;
  tamano: number;
  totalPaginas: number;
  vehiculos: Vehiculo[];
}

export interface BusquedaDisponibilidad {
  sucursal: string;
  categoria: string;
  fechaInicio: string; // AAAA-MM-DD
  fechaFin: string; // AAAA-MM-DD
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

// Traduce los códigos de error del backend (los del documento) a mensajes para el usuario.
function mensajeDeError(codigo: string | undefined, mensaje: string | undefined): string {
  switch (codigo) {
    case "VEHICULO_NO_ENCONTRADO":
      return "No existe un vehículo con esa placa.";
    case "VEHICULO_NO_DISPONIBLE":
      return "El vehículo no está disponible: ya está alquilado o en mantenimiento.";
    case "ROL_NO_AUTORIZADO":
      return "Tu rol no puede cambiar el estado de un vehículo.";
    case "SOLICITUD_INVALIDA":
      return mensaje ?? "Revisa los datos ingresados.";
    case "LIMITE_EXCEDIDO":
      return "Hiciste demasiadas consultas seguidas. Espera unos segundos e inténtalo de nuevo.";
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
    response = await fetch(`${API_BASE_URL}/api/v1/flota${ruta}`, {
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
    if (response.status >= 500) {
      throw new Error(
        `El servidor respondió con HTTP ${response.status}. Revisa que el servicio de flota esté disponible.`,
      );
    }
    const err = isRecord(datos) ? datos : {};
    throw new Error(
      mensajeDeError(
        typeof err.codigo === "string" ? err.codigo : undefined,
        typeof err.mensaje === "string" ? err.mensaje : undefined,
      ),
    );
  }

  if (datos === null) {
    throw new Error("El servidor devolvió una respuesta que no se pudo leer.");
  }
  return datos as T;
}

/** GET /api/v1/flota/disponibilidad — el gateway la limita a 200 consultas por minuto. */
export function consultarDisponibilidad(
  sesion: Sesion,
  busqueda: BusquedaDisponibilidad,
): Promise<Disponibilidad> {
  const params = new URLSearchParams({
    sucursal: busqueda.sucursal,
    categoria: busqueda.categoria,
    fechaInicio: busqueda.fechaInicio,
    fechaFin: busqueda.fechaFin,
  });
  return solicitar<Disponibilidad>(sesion, `/disponibilidad?${params.toString()}`);
}

/** GET /api/v1/flota/vehiculos/{placa} */
export function consultarVehiculo(sesion: Sesion, placa: string): Promise<Vehiculo> {
  return solicitar<Vehiculo>(sesion, `/vehiculos/${encodeURIComponent(placa.trim())}`);
}

/** PUT /api/v1/flota/vehiculos/{placa}/estado — solo AGENTE o ADMINISTRADOR. El motivo es opcional y queda en el historial. */
export function actualizarEstado(
  sesion: Sesion,
  placa: string,
  estado: EstadoVehiculo,
  motivo?: string,
): Promise<Vehiculo> {
  const texto = motivo?.trim();
  return solicitar<Vehiculo>(sesion, `/vehiculos/${encodeURIComponent(placa)}/estado`, {
    method: "PUT",
    body: texto ? { estado, motivo: texto } : { estado },
  });
}

/** GET /api/v1/flota/vehiculos — una página de vehículos por estado (por defecto DISPONIBLE), sucursal y marca/modelo. */
export function listarVehiculos(sesion: Sesion, filtro: FiltroListado = {}): Promise<PaginaVehiculos> {
  const params = new URLSearchParams();
  if (filtro.estado) params.set("estado", filtro.estado);
  if (filtro.sucursal) params.set("sucursal", filtro.sucursal);
  if (filtro.texto?.trim()) params.set("texto", filtro.texto.trim());
  if (filtro.pagina !== undefined) params.set("pagina", String(filtro.pagina));
  if (filtro.tamano !== undefined) params.set("tamano", String(filtro.tamano));
  const query = params.toString();
  return solicitar<PaginaVehiculos>(sesion, `/vehiculos${query ? `?${query}` : ""}`);
}

/** GET /api/v1/flota/vehiculos/{placa}/historial — del cambio más reciente al más antiguo. */
export function consultarHistorial(sesion: Sesion, placa: string): Promise<CambioHistorial[]> {
  return solicitar<CambioHistorial[]>(sesion, `/vehiculos/${encodeURIComponent(placa)}/historial`);
}
