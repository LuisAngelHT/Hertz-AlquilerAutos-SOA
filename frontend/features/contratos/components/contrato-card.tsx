import { useEffect, useState } from "react";
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
import { ContratoFirmado } from "@/features/contratos/components/contrato-firmado";
import { RevisionesContrato } from "@/features/contratos/components/revisiones-contrato";
import type { Sesion } from "@/features/flota/lib/session";
import { consultarReserva, type Reserva } from "@/features/reservas/lib/reservas-api";

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

/** Muestra el ID abreviado; al hacer clic copia el ID completo (sirve para pegarlo en los filtros). */
function IdCopiable({ id, prefijo }: { id: string; prefijo: string }) {
  const [copiado, setCopiado] = useState(false);

  async function copiar() {
    try {
      await navigator.clipboard.writeText(id);
      setCopiado(true);
      setTimeout(() => setCopiado(false), 1800);
    } catch {
      /* sin permiso del portapapeles: el ID completo sigue visible al pasar el mouse */
    }
  }

  return (
    <button
      type="button"
      onClick={copiar}
      title={`Clic para copiar el ID completo: ${id}`}
      className="inline-flex items-center gap-1.5 rounded-md bg-zinc-100 px-2 py-0.5 font-mono text-zinc-700 transition-colors hover:bg-amber-100 focus-visible:outline focus-visible:outline-2 focus-visible:outline-amber-500"
    >
      {prefijo}
      {id.slice(0, 8).toUpperCase()}
      <span className="font-sans text-[10px] font-semibold text-amber-700">
        {copiado ? "¡Copiado!" : "Copiar"}
      </span>
    </button>
  );
}

/** Código corto y legible para mostrar (el completo sigue disponible al pasar el mouse). */
function codigoCorto(id: string): string {
  return id.slice(0, 8).toUpperCase();
}

interface ContratoCardProps {
  contrato: Contrato;
  sesion: Sesion;
  onSesionExpirada: () => void;
}

export function ContratoCard({ contrato, sesion, onSesionExpirada }: ContratoCardProps) {
  const vigente = contrato.estado === "VIGENTE";
  // Quién alquiló: el contrato solo guarda el código del cliente, así que los datos
  // (nombre, documento, teléfono) se toman de la reserva a la que pertenece.
  const [reserva, setReserva] = useState<Reserva | null>(null);

  useEffect(() => {
    let activa = true;
    consultarReserva(sesion, contrato.idReserva)
      .then((datos) => {
        if (activa) setReserva(datos);
      })
      .catch(() => {
        /* si no se puede leer, la tarjeta sigue mostrando el código del cliente */
      });
    return () => {
      activa = false;
    };
  }, [sesion, contrato.idReserva]);

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
              Contrato N.º {codigoCorto(contrato.idContrato)}
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
              <dt className="text-xs text-zinc-400">Entregado</dt>
              <dd className="mt-0.5 font-medium text-zinc-800">
                {formatoFechaHora(contrato.entrega?.registradoEn ?? contrato.creadoEn)}
              </dd>
            </div>
          </div>

          <div className="flex items-start gap-2 text-zinc-600">
            <CalendarDays className="mt-0.5 size-4 shrink-0 text-zinc-400" aria-hidden="true" />
            <div>
              <dt className="text-xs text-zinc-400">Devuelto</dt>
              <dd className="mt-0.5 font-medium text-zinc-800">
                {contrato.devolucion
                  ? formatoFechaHora(contrato.devolucion.registradoEn)
                  : contrato.fechaDevolucion
                    ? formatoFecha(contrato.fechaDevolucion)
                    : "Pendiente"}
              </dd>
            </div>
          </div>

          <div className="flex items-start gap-2 text-zinc-600">
            <UserRound className="mt-0.5 size-4 shrink-0 text-zinc-400" aria-hidden="true" />
            <div>
              <dt className="text-xs text-zinc-400">Registrado por (personal Hertz)</dt>
              <dd className="mt-0.5 break-all font-medium text-zinc-800">
                {contrato.registradoPor}
              </dd>
            </div>
          </div>
        </dl>

        <div className="grid gap-3 border-t border-zinc-100 pt-4 text-sm sm:grid-cols-2">
          <div className="flex items-start gap-2 text-zinc-600 sm:col-span-2">
            <UserRound className="mt-0.5 size-4 shrink-0 text-zinc-400" aria-hidden="true" />
            <div>
              <dt className="text-xs text-zinc-400">Cliente que se lleva el vehículo</dt>
              <dd className="mt-0.5 font-medium text-zinc-800">
                {reserva?.clienteNombre ? (
                  <>
                    {reserva.clienteNombre}
                    {reserva.clienteDocumento ? (
                      <span className="font-normal text-zinc-500"> · Doc. {reserva.clienteDocumento}</span>
                    ) : null}
                    {reserva.clienteTelefono ? (
                      <span className="font-normal text-zinc-500"> · Tel. {reserva.clienteTelefono}</span>
                    ) : null}
                  </>
                ) : (
                  <span>Cliente sin datos de reserva</span>
                )}
              </dd>
            </div>
          </div>
        </div>

        <RevisionesContrato contrato={contrato} />

        <ContratoFirmado contrato={contrato} sesion={sesion} onSesionExpirada={onSesionExpirada} />

        <div className="grid gap-2 border-t border-zinc-100 pt-4 text-xs text-zinc-500 sm:grid-cols-2">
          <p className="flex flex-wrap items-center gap-2">
            ID de la reserva:
            <IdCopiable id={contrato.idReserva} prefijo="" />
          </p>
          <p className="flex flex-wrap items-center gap-2">
            ID del cliente:
            <IdCopiable id={contrato.idCliente} prefijo="" />
          </p>
          <p>Creado: {formatoFechaHora(contrato.creadoEn)}</p>
        </div>
      </CardContent>
    </Card>
  );
}
