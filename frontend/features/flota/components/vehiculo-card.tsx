"use client";

import { useState } from "react";
import { CarFront, LoaderCircle, MapPin } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  actualizarEstado,
  SesionExpiradaError,
  type EstadoVehiculo,
  type Vehiculo,
} from "@/features/flota/lib/flota-api";
import { etiquetaCategoria, etiquetaSucursal } from "@/features/flota/lib/constants";
import type { Sesion } from "@/features/flota/lib/session";

// Apariencia de cada estado (color de la etiqueta y texto).
const ESTADOS: Record<EstadoVehiculo, { texto: string; clases: string }> = {
  DISPONIBLE: {
    texto: "Disponible",
    clases: "bg-emerald-50 text-emerald-700 ring-emerald-200",
  },
  ALQUILADO: {
    texto: "Alquilado",
    clases: "bg-amber-50 text-amber-800 ring-amber-200",
  },
  MANTENIMIENTO: {
    texto: "En mantenimiento",
    clases: "bg-red-50 text-red-700 ring-red-200",
  },
};

// Qué cambios de estado se ofrecen según el estado actual. La regla de fondo
// (solo se alquila lo que está DISPONIBLE) la valida igual el backend.
const ACCIONES: Record<
  EstadoVehiculo,
  { destino: EstadoVehiculo; etiqueta: string; principal?: boolean }[]
> = {
  DISPONIBLE: [
    { destino: "ALQUILADO", etiqueta: "Alquilar", principal: true },
    { destino: "MANTENIMIENTO", etiqueta: "Enviar a mantenimiento" },
  ],
  ALQUILADO: [
    { destino: "DISPONIBLE", etiqueta: "Registrar devolución", principal: true },
    { destino: "MANTENIMIENTO", etiqueta: "Enviar a mantenimiento" },
  ],
  MANTENIMIENTO: [
    { destino: "DISPONIBLE", etiqueta: "Marcar disponible", principal: true },
  ],
};

interface VehiculoCardProps {
  vehiculo: Vehiculo;
  sesion: Sesion;
  /** true si el rol del usuario puede cambiar el estado (AGENTE o ADMINISTRADOR). */
  puedeEditar: boolean;
  /** Se llama con el vehículo ya actualizado, para que el padre refresque su lista. */
  onActualizado: (vehiculo: Vehiculo) => void;
  onSesionExpirada: () => void;
}

export function VehiculoCard({
  vehiculo,
  sesion,
  puedeEditar,
  onActualizado,
  onSesionExpirada,
}: VehiculoCardProps) {
  const [pendiente, setPendiente] = useState<EstadoVehiculo | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function cambiarEstado(destino: EstadoVehiculo) {
    setError(null);
    setPendiente(destino);
    try {
      const actualizado = await actualizarEstado(sesion, vehiculo.placa, destino);
      onActualizado(actualizado);
    } catch (cause) {
      if (cause instanceof SesionExpiradaError) {
        onSesionExpirada();
        return;
      }
      setError(cause instanceof Error ? cause.message : "No se pudo cambiar el estado.");
    } finally {
      setPendiente(null);
    }
  }

  const estado = ESTADOS[vehiculo.estado];

  return (
    <Card className="rounded-2xl border-0 bg-white ring-1 ring-zinc-200/80">
      <CardContent className="space-y-4 p-5">
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-3">
            <span className="grid size-11 place-items-center rounded-xl bg-[#fff4b8] text-zinc-950">
              <CarFront className="size-6" aria-hidden="true" />
            </span>
            <div>
              <p className="text-base font-semibold tracking-tight text-zinc-950">
                {vehiculo.marca} {vehiculo.modelo}
              </p>
              <p className="font-mono text-sm tracking-wide text-zinc-500">
                {vehiculo.placa}
              </p>
            </div>
          </div>
          <span
            className={`shrink-0 rounded-full px-3 py-1 text-xs font-semibold ring-1 ${estado.clases}`}
          >
            {estado.texto}
          </span>
        </div>

        <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
          <div>
            <dt className="text-xs text-zinc-400">Categoría</dt>
            <dd className="font-medium text-zinc-800">
              {etiquetaCategoria(vehiculo.categoria)}
            </dd>
          </div>
          <div>
            <dt className="text-xs text-zinc-400">Año</dt>
            <dd className="font-medium text-zinc-800">{vehiculo.anio}</dd>
          </div>
          <div className="col-span-2">
            <dt className="text-xs text-zinc-400">Sucursal</dt>
            <dd className="flex items-center gap-1.5 font-medium text-zinc-800">
              <MapPin className="size-3.5 text-zinc-400" aria-hidden="true" />
              {etiquetaSucursal(vehiculo.sucursal)}
            </dd>
          </div>
        </dl>

        {error && (
          <div
            role="alert"
            className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm leading-5 text-red-700"
          >
            {error}
          </div>
        )}

        {puedeEditar && (
          <div className="flex flex-wrap gap-2 border-t border-zinc-100 pt-4">
            {ACCIONES[vehiculo.estado].map((accion) => (
              <Button
                key={accion.destino}
                type="button"
                disabled={pendiente !== null}
                onClick={() => cambiarEstado(accion.destino)}
                variant={accion.principal ? "default" : "outline"}
                className={
                  accion.principal
                    ? "h-10 rounded-xl bg-[#ffd400] px-4 font-bold text-zinc-950 hover:bg-[#f2c900]"
                    : "h-10 rounded-xl border-zinc-200 px-4"
                }
              >
                {pendiente === accion.destino && (
                  <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
                )}
                {accion.etiqueta}
              </Button>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
