import {
  CalendarDays,
  CarFront,
  FileSignature,
  MapPin,
  UserRound,
} from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { etiquetaSucursal } from "@/features/flota/lib/constants";
import type { Contrato } from "@/features/contratos/types/contrato";

function formatoFecha(fecha: string): string {
  const valor = new Date(`${fecha}T12:00:00`);
  return Number.isNaN(valor.getTime())
    ? fecha
    : new Intl.DateTimeFormat("es-PE", { dateStyle: "medium" }).format(valor);
}

function formatoFechaHora(fecha: string): string {
  const valor = new Date(fecha);
  return Number.isNaN(valor.getTime())
    ? fecha
    : new Intl.DateTimeFormat("es-PE", {
        dateStyle: "medium",
        timeStyle: "short",
      }).format(valor);
}

export function ContratoCard({ contrato }: { contrato: Contrato }) {
  const vigente = contrato.estado === "VIGENTE";

  return (
    <Card className="rounded-2xl border-0 bg-white shadow-[0_14px_44px_-30px_rgba(24,24,27,0.35)] ring-1 ring-zinc-200/80">
      <CardContent className="space-y-5 p-5 sm:p-6">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p className="inline-flex items-center gap-2 font-mono text-lg font-bold tracking-wide text-zinc-950">
              <CarFront className="size-5 text-amber-600" aria-hidden="true" />
              {contrato.placa}
            </p>
            <p className="mt-1 text-xs text-zinc-400">
              Contrato {contrato.idContrato.slice(0, 8).toUpperCase()}
            </p>
          </div>
          <span
            className={`rounded-full px-3 py-1 text-xs font-semibold ring-1 ${
              vigente
                ? "bg-amber-50 text-amber-800 ring-amber-200"
                : "bg-emerald-50 text-emerald-700 ring-emerald-200"
            }`}
          >
            {vigente ? "Vigente" : "Finalizado"}
          </span>
        </div>

        <dl className="grid gap-4 border-t border-zinc-100 pt-4 text-sm sm:grid-cols-2">
          <div className="flex items-start gap-2 text-zinc-600">
            <CalendarDays className="mt-0.5 size-4 shrink-0 text-zinc-400" aria-hidden="true" />
            <div>
              <dt className="text-xs text-zinc-400">Periodo previsto</dt>
              <dd className="mt-0.5 font-medium text-zinc-800">
                {formatoFecha(contrato.fechaInicio)} – {formatoFecha(contrato.fechaFinPrevista)}
              </dd>
            </div>
          </div>

          <div className="flex items-start gap-2 text-zinc-600">
            <MapPin className="mt-0.5 size-4 shrink-0 text-zinc-400" aria-hidden="true" />
            <div>
              <dt className="text-xs text-zinc-400">Recojo y devolución</dt>
              <dd className="mt-0.5 font-medium text-zinc-800">
                {etiquetaSucursal(contrato.sucursalRecojo)}
                <span className="px-1 text-zinc-400">→</span>
                {etiquetaSucursal(contrato.sucursalDevolucion)}
              </dd>
            </div>
          </div>

          <div className="flex items-start gap-2 text-zinc-600">
            <FileSignature className="mt-0.5 size-4 shrink-0 text-zinc-400" aria-hidden="true" />
            <div>
              <dt className="text-xs text-zinc-400">Devolución real</dt>
              <dd className="mt-0.5 font-medium text-zinc-800">
                {contrato.fechaDevolucion
                  ? formatoFecha(contrato.fechaDevolucion)
                  : "Pendiente"}
              </dd>
            </div>
          </div>

          <div className="flex items-start gap-2 text-zinc-600">
            <UserRound className="mt-0.5 size-4 shrink-0 text-zinc-400" aria-hidden="true" />
            <div>
              <dt className="text-xs text-zinc-400">Registrado por</dt>
              <dd className="mt-0.5 break-all font-medium text-zinc-800">
                {contrato.registradoPor}
              </dd>
            </div>
          </div>
        </dl>

        <div className="grid gap-2 border-t border-zinc-100 pt-4 text-xs text-zinc-500 sm:grid-cols-2">
          <p>
            Reserva: <span className="break-all font-mono text-zinc-700">{contrato.idReserva}</span>
          </p>
          <p>
            Cliente: <span className="break-all font-mono text-zinc-700">{contrato.idCliente}</span>
          </p>
          <p className="sm:col-span-2">Creado: {formatoFechaHora(contrato.creadoEn)}</p>
        </div>
      </CardContent>
    </Card>
  );
}
