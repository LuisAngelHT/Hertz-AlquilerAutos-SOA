"use client";

import { useEffect, useId, type ReactNode } from "react";
import { createPortal } from "react-dom";
import { X } from "lucide-react";

interface VentanaModalProps {
  titulo: string;
  subtitulo?: ReactNode;
  onCerrar: () => void;
  /** Mientras algo se está guardando no se deja cerrar (ni con Esc ni haciendo clic fuera). */
  ocupado?: boolean;
  /** Clase de ancho máximo (por defecto max-w-lg). */
  ancho?: string;
  children: ReactNode;
}

/** Ventana emergente accesible: se cierra con Esc o con un clic fuera, y bloquea el cierre mientras trabaja. */
export function VentanaModal({
  titulo,
  subtitulo,
  onCerrar,
  ocupado = false,
  ancho = "max-w-lg",
  children,
}: VentanaModalProps) {
  const tituloId = useId();

  useEffect(() => {
    function alPulsar(event: KeyboardEvent) {
      if (event.key === "Escape" && !ocupado) onCerrar();
    }
    document.addEventListener("keydown", alPulsar);
    return () => document.removeEventListener("keydown", alPulsar);
  }, [ocupado, onCerrar]);

  return createPortal(
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-zinc-950/55 p-4"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !ocupado) onCerrar();
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={tituloId}
        className={`max-h-[94vh] w-full ${ancho} overflow-y-auto rounded-3xl bg-white p-5 shadow-2xl ring-1 ring-zinc-200 sm:p-7`}
      >
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <h2 id={tituloId} className="text-lg font-semibold tracking-tight text-zinc-950">
              {titulo}
            </h2>
            {subtitulo ? <div className="mt-0.5 text-sm text-zinc-600">{subtitulo}</div> : null}
          </div>
          <button
            type="button"
            onClick={onCerrar}
            disabled={ocupado}
            aria-label="Cerrar"
            className="rounded-lg p-1.5 text-zinc-500 hover:bg-zinc-100 disabled:opacity-40"
          >
            <X className="size-5" aria-hidden="true" />
          </button>
        </div>
        {children}
      </div>
    </div>,
    document.body,
  );
}
