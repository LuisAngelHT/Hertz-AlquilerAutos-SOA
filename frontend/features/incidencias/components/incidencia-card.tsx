"use client";

import { useState } from "react";
import {
  CalendarClock,
  CarFront,
  CheckCircle2,
  CircleDollarSign,
  LoaderCircle,
  MapPin,
  UserRound,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { etiquetaSucursal } from "@/features/flota/lib/constants";
import type { Incidencia } from "@/features/incidencias/types/incidencia";

const TIPO_ETIQUETA: Record<Incidencia["tipo"], string> = {
  "DAÑO": "Daño",
  MANTENIMIENTO: "Mantenimiento",
  SINIESTRO: "Siniestro",
};

function formatoFecha(fecha: string): string {
  const valor = new Date(fecha);
  return Number.isNaN(valor.getTime())
    ? fecha
    : new Intl.DateTimeFormat("es-PE", {
        dateStyle: "medium",
        timeStyle: "short",
      }).format(valor);
}

interface IncidenciaCardProps {
  incidencia: Incidencia;
  /** Si se pasa, una incidencia ABIERTA muestra el botón "Cerrar incidencia". Debe lanzar un Error con el motivo si falla. */
  onCerrar?: (incidencia: Incidencia) => Promise<void>;
}

export function IncidenciaCard({ incidencia, onCerrar }: IncidenciaCardProps) {
  const [cerrando, setCerrando] = useState(false);
  const [errorCierre, setErrorCierre] = useState<string | null>(null);

  async function cerrar() {
    if (!onCerrar) return;
    setCerrando(true);
    setErrorCierre(null);
    try {
      await onCerrar(incidencia);
    } catch (cause) {
      setErrorCierre(cause instanceof Error ? cause.message : "No se pudo cerrar la incidencia.");
    } finally {
      setCerrando(false);
    }
  }

  return (
    <Card className="rounded-2xl border-0 bg-white shadow-[0_14px_44px_-30px_rgba(24,24,27,0.35)] ring-1 ring-zinc-200/80">
      <CardContent className="space-y-4 p-5">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p className="inline-flex items-center gap-2 font-mono text-lg font-bold tracking-wide text-zinc-950">
              <CarFront className="size-5 text-amber-600" aria-hidden="true" />
              {incidencia.idVehiculo}
            </p>
            <p className="mt-1 text-xs text-zinc-400">
              Incidencia {incidencia.idIncidencia.slice(0, 8).toUpperCase()}
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <span className="rounded-full bg-amber-50 px-3 py-1 text-xs font-semibold text-amber-800 ring-1 ring-amber-200">
              {TIPO_ETIQUETA[incidencia.tipo]}
            </span>
            <span
              className={`rounded-full px-3 py-1 text-xs font-semibold ring-1 ${
                incidencia.estado === "ABIERTA"
                  ? "bg-red-50 text-red-700 ring-red-200"
                  : "bg-emerald-50 text-emerald-700 ring-emerald-200"
              }`}
            >
              {incidencia.estado === "ABIERTA" ? "Abierta" : "Cerrada"}
            </span>
          </div>
        </div>

        <p className="whitespace-pre-wrap text-sm leading-6 text-zinc-700">
          {incidencia.descripcion}
        </p>

        <dl className="grid gap-3 border-t border-zinc-100 pt-4 text-sm sm:grid-cols-2">
          <div className="flex items-start gap-2 text-zinc-600">
            <MapPin className="mt-0.5 size-4 shrink-0 text-zinc-400" aria-hidden="true" />
            <div>
              <dt className="sr-only">Sucursal</dt>
              <dd>{etiquetaSucursal(incidencia.sucursal)}</dd>
            </div>
          </div>
          <div className="flex items-start gap-2 text-zinc-600">
            <CalendarClock className="mt-0.5 size-4 shrink-0 text-zinc-400" aria-hidden="true" />
            <div>
              <dt className="sr-only">Registrada</dt>
              <dd>{formatoFecha(incidencia.creadoEn)}</dd>
            </div>
          </div>
          <div className="flex items-start gap-2 text-zinc-600">
            <CircleDollarSign className="mt-0.5 size-4 shrink-0 text-zinc-400" aria-hidden="true" />
            <div>
              <dt className="sr-only">Costo estimado</dt>
              <dd>
                {incidencia.costoEstimado === null
                  ? "Costo no estimado"
                  : new Intl.NumberFormat("es-PE", {
                      style: "currency",
                      currency: "PEN",
                    }).format(incidencia.costoEstimado)}
              </dd>
            </div>
          </div>
          <div className="flex items-start gap-2 text-zinc-600">
            <UserRound className="mt-0.5 size-4 shrink-0 text-zinc-400" aria-hidden="true" />
            <div>
              <dt className="sr-only">Registrada por</dt>
              <dd className="break-all">{incidencia.registradaPor}</dd>
            </div>
          </div>
        </dl>

        {incidencia.idContrato && (
          <p className="border-t border-zinc-100 pt-3 text-xs text-zinc-500">
            Contrato: <span className="font-mono">{incidencia.idContrato}</span>
          </p>
        )}

        {onCerrar && incidencia.estado === "ABIERTA" && (
          <div className="space-y-2 border-t border-zinc-100 pt-4">
            <div className="flex flex-wrap items-center gap-3">
              <Button
                type="button"
                disabled={cerrando}
                onClick={cerrar}
                className="h-9 rounded-xl bg-[#ffd400] px-4 text-sm font-semibold text-zinc-950 hover:bg-[#f2c900]"
              >
                {cerrando ? (
                  <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
                ) : (
                  <CheckCircle2 className="size-4" aria-hidden="true" />
                )}
                Cerrar incidencia
              </Button>
              <p className="text-xs text-zinc-500">
                Si es la última abierta de este vehículo, vuelve a quedar disponible.
              </p>
            </div>
            {errorCierre && (
              <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
                {errorCierre}
              </p>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
