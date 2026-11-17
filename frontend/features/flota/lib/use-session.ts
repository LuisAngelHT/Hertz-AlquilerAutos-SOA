"use client";

import { useMemo, useSyncExternalStore } from "react";
import { leerSesionCruda, parseSesion, type Sesion } from "./session";

// Suscripción mínima: sessionStorage no avisa cambios dentro de la misma pestaña.
const oyentes = new Set<() => void>();

function suscribir(avisar: () => void) {
  oyentes.add(avisar);
  return () => {
    oyentes.delete(avisar);
  };
}

/** Avisa a los componentes que la sesión cambió (por ejemplo, al cerrar sesión). */
export function notificarCambioSesion() {
  oyentes.forEach((avisar) => avisar());
}

/**
 * Lee la sesión del login.
 *  - cargando = true: todavía se está en el servidor / hidratando; aún no se sabe si hay sesión.
 *  - cargando = false y sesion = null: no hay sesión válida (hay que mandar al login).
 */
export function useSesion(): { cargando: boolean; sesion: Sesion | null } {
  const raw = useSyncExternalStore<string | null | undefined>(
    suscribir,
    leerSesionCruda,
    () => undefined,
  );

  return useMemo(
    () =>
      raw === undefined
        ? { cargando: true, sesion: null }
        : { cargando: false, sesion: parseSesion(raw) },
    [raw],
  );
}
