"use client";

import { useState } from "react";
import {
  CalendarClock,
  CarFront,
  CircleDollarSign,
  ClipboardList,
  MapPin,
  UserRound,
  Wrench,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { etiquetaSucursal } from "@/features/flota/lib/constants";
import type { Sesion } from "@/features/flota/lib/session";
import { FichaTallerModal } from "@/features/incidencias/components/ficha-taller-modal";
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

function fechaCorta(iso: string): string {
  const valor = new Date(`${iso}T12:00:00`);
  return Number.isNaN(valor.getTime()) ? iso : new Intl.DateTimeFormat("es-PE", { dateStyle: "medium" }).format(valor);
}

interface IncidenciaCardProps {
  incidencia: Incidencia;
  sesion: Sesion;
  /** Se llama con la incidencia ya actualizada (ficha guardada o incidencia cerrada). */
  onActualizada: (incidencia: Incidencia) => void;
  onSesionExpirada: () => void;
}

export function IncidenciaCard({ incidencia, sesion, onActualizada, onSesionExpirada }: IncidenciaCardProps) {
  const [fichaAbierta, setFichaAbierta] = useState(false);
  const abierta = incidencia.estado === "ABIERTA";

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
              {abierta ? "Abierta · en taller" : "Cerrada"}
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

        <div className="space-y-2 border-t border-zinc-100 pt-4">
          <div className="flex flex-wrap items-center gap-3">
            <Button
              type="button"
              onClick={() => setFichaAbierta(true)}
              className="h-9 rounded-xl bg-[#ffd400] px-4 text-sm font-semibold text-zinc-950 hover:bg-[#f2c900]"
            >
              <ClipboardList className="size-4" aria-hidden="true" />
              {abierta ? "Ficha del taller" : "Ver ficha del taller"}
            </Button>
            {abierta && (
              <p className="flex items-center gap-1.5 text-xs text-zinc-500">
                <Wrench className="size-3.5 text-zinc-400" aria-hidden="true" />
                {incidencia.fechaSalidaPrevista
                  ? `Sale del taller el ${fechaCorta(incidencia.fechaSalidaPrevista)}`
                  : incidencia.tieneFicha
                    ? "Aún sin días estimados de reparación"
                    : "Sin diagnóstico todavía: abre la ficha para registrarlo"}
              </p>
            )}
          </div>
        </div>

        {fichaAbierta && (
          <FichaTallerModal
            incidencia={incidencia}
            sesion={sesion}
            onCerrar={() => setFichaAbierta(false)}
            onActualizada={onActualizada}
            onSesionExpirada={onSesionExpirada}
          />
        )}
      </CardContent>
    </Card>
  );
}
