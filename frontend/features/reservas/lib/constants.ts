import type { EstadoReserva } from "./reservas-api";

/** Estilos y textos de cada estado de reserva. */
export const ESTADOS_RESERVA: Record<EstadoReserva, { etiqueta: string; clases: string }> = {
  CONFIRMADA: { etiqueta: "Confirmada", clases: "bg-emerald-50 text-emerald-700 ring-emerald-200" },
  EN_CURSO: { etiqueta: "En curso", clases: "bg-sky-50 text-sky-700 ring-sky-200" },
  FINALIZADA: { etiqueta: "Finalizada", clases: "bg-zinc-100 text-zinc-600 ring-zinc-200" },
  CANCELADA: { etiqueta: "Cancelada", clases: "bg-red-50 text-red-700 ring-red-200" },
};

/** Hoy en hora de Lima, como AAAA-MM-DD (para el mínimo de los campos de fecha). */
export function hoyEnLima(): string {
  return new Date().toLocaleDateString("en-CA", { timeZone: "America/Lima" });
}

/** "2026-12-05" -> "05/12/2026" sin pasar por Date (evita el desfase de zona horaria). */
export function formatearFecha(iso: string): string {
  const [anio, mes, dia] = iso.split("-");
  return anio && mes && dia ? `${dia}/${mes}/${anio}` : iso;
}

export function formatearFechaHora(iso: string): string {
  const fecha = new Date(iso);
  if (Number.isNaN(fecha.getTime())) return iso;
  return fecha.toLocaleString("es-PE", {
    timeZone: "America/Lima",
    dateStyle: "short",
    timeStyle: "short",
  });
}
