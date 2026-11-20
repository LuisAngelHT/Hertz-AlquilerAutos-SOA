"use client";

import { useState } from "react";
import { AutoDismissFeedback } from "@/components/auto-dismiss-feedback";
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
import type { Sesion } from "@/features/flota/lib/session";
import {
  cerrarIncidencia,
  mensajeErrorIncidencia,
} from "@/features/incidencias/lib/incidencias-api";
import { etiquetaSucursal } from "@/features/flota/lib/constants";
import type { Incidencia } from "@/features/incidencias/types/incidencia";
import { ApiRequestError } from "@/lib/api-client";

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
  sesion: Sesion;
  onCerrada: () => void;
  onSesionExpirada: () => void;
}

export function IncidenciaCard({
  incidencia,
  sesion,
  onCerrada,
  onSesionExpirada,
}: IncidenciaCardProps) {
  const [cerrando, setCerrando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [mensaje, setMensaje] = useState<string | null>(null);

  async function cerrar() {
    setCerrando(true);
    setError(null);
    setMensaje(null);
    try {
      const resultado = await cerrarIncidencia(sesion.token, incidencia.idIncidencia);
      setMensaje(
        resultado.estadoVehiculo === "MANTENIMIENTO"
          ? "Incidencia cerrada; el vehículo permanece en mantenimiento porque tiene otra incidencia abierta."
          : `Incidencia cerrada; vehículo ${resultado.estadoVehiculo.toLowerCase()}.`,
      );
      onCerrada();
    } catch (cause) {
      if (cause instanceof ApiRequestError && cause.status === 401) {
        onSesionExpirada();
        return;
      }
      setError(mensajeErrorIncidencia(cause));
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
        {error && (
          <AutoDismissFeedback key={error} role="alert" className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            {error}
          </AutoDismissFeedback>
        )}
        {mensaje && (
          <AutoDismissFeedback key={mensaje} role="status" className="rounded-xl border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-800">
            {mensaje}
          </AutoDismissFeedback>
        )}
        {incidencia.estado === "ABIERTA" && (
          <Button
            type="button"
            disabled={cerrando}
            onClick={cerrar}
            className="h-10 rounded-xl bg-emerald-600 px-4 font-semibold text-white hover:bg-emerald-700"
          >
            {cerrando ? (
              <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
            ) : (
              <CheckCircle2 className="size-4" aria-hidden="true" />
            )}
            Cerrar incidencia
          </Button>
        )}
      </CardContent>
    </Card>
  );
}
