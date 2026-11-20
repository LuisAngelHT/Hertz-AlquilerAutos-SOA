/**
 * Sesión del usuario, tal como la guarda el login (features/auth/components/login-form.tsx)
 * en sessionStorage. Aquí solo se LEE: el login es quien la crea.
 */

// Misma clave que usa el login. Si cambia allá, hay que cambiarla aquí.
export const SESSION_KEY = "hertz-auth-session";

export interface Sesion {
  token: string;
  tipo: string;
  expiraEn: string;
  rol: string;
  nombre: string;
  /** Sede del usuario; null si no tiene (por ejemplo el ADMINISTRADOR general). */
  sucursal: string | null;
}

/** Texto crudo de la sesión guardada, o null si no hay (o si el navegador bloquea el storage). */
export function leerSesionCruda(): string | null {
  try {
    return window.sessionStorage.getItem(SESSION_KEY);
  } catch {
    return null;
  }
}

/** Convierte el texto guardado en una sesión válida. Devuelve null si falta, está mal o ya venció. */
export function parseSesion(raw: string | null): Sesion | null {
  if (!raw) return null;
  try {
    const data: unknown = JSON.parse(raw);
    if (typeof data !== "object" || data === null) return null;
    const s = data as Record<string, unknown>;
    if (
      typeof s.token !== "string" ||
      typeof s.tipo !== "string" ||
      typeof s.expiraEn !== "string" ||
      typeof s.rol !== "string" ||
      typeof s.nombre !== "string"
    ) {
      return null;
    }
    const vence = new Date(s.expiraEn).getTime();
    if (Number.isNaN(vence) || vence <= Date.now()) return null;
    return {
      token: s.token,
      tipo: s.tipo,
      expiraEn: s.expiraEn,
      rol: s.rol,
      nombre: s.nombre,
      sucursal: typeof s.sucursal === "string" && s.sucursal ? s.sucursal : null,
    };
  } catch {
    return null;
  }
}

export function cerrarSesion(): void {
  try {
    window.sessionStorage.removeItem(SESSION_KEY);
  } catch {
    // Si el navegador bloquea el storage no hay nada que borrar.
  }
}

/** Solo estos roles (AGENTE, ADMINISTRADOR, ADMINISTRADOR_SUCURSAL) pueden cambiar el estado de un vehículo (lo valida también el backend). */
export function puedeCambiarEstado(rol: string): boolean {
  return rol === "AGENTE" || rol === "ADMINISTRADOR" || rol === "ADMINISTRADOR_SUCURSAL";
}

/**
 * ¿Puede este usuario cambiar el estado de un vehículo de la sucursal indicada?
 * El ADMINISTRADOR general puede con cualquiera; el AGENTE y el ADMINISTRADOR_SUCURSAL solo con
 * los de su propia sucursal. El backend aplica la misma regla (esto solo decide qué botones se ven).
 */
export function puedeCambiarVehiculo(sesion: Sesion, sucursalVehiculo: string): boolean {
  if (!puedeCambiarEstado(sesion.rol)) return false;
  if (sesion.rol === "ADMINISTRADOR") return true;
  return sesion.sucursal !== null && sesion.sucursal === sucursalVehiculo;
}

/** Roles de escritura que deben tener una sucursal asignada para poder modificar vehículos. */
export function requiereSucursal(rol: string): boolean {
  return puedeCambiarEstado(rol) && rol !== "ADMINISTRADOR";
}
