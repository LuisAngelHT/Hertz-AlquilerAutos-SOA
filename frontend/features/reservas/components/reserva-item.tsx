"use client";

import { useState } from "react";
import { CalendarDays, CarFront, History, LoaderCircle, MapPin, UserRound } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { etiquetaCategoria, etiquetaSucursal } from "@/features/flota/lib/constants";
import type { Sesion } from "@/features/flota/lib/session";
import {
  ESTADOS_RESERVA,
  formatearFecha,
  formatearFechaHora,
  hoyEnLima,
} from "@/features/reservas/lib/constants";
import {
  cancelarReserva,
  consultarHistorialReserva,
  modificarReserva,
  SesionExpiradaError,
  type CambioReserva,
  type Reserva,
} from "@/features/reservas/lib/reservas-api";

const CAMPO_FECHA =
  "h-10 w-full rounded-lg border border-zinc-200 bg-white px-3 text-sm text-zinc-900 outline-none focus-visible:border-amber-400 focus-visible:ring-2 focus-visible:ring-amber-300/40";

interface ReservaItemProps {
  reserva: Reserva;
  sesion: Sesion;
  /** Personal: ve el vehículo asignado, el historial y puede hacer check-out y devolución. */
  esPersonal: boolean;
  onActualizada: (reserva: Reserva) => void;
  onSesionExpirada: () => void;
}

type Modo = "ninguno" | "modificar" | "cancelar";

/** Una reserva con sus acciones: modificar fechas, cancelar, entregar (check-out), devolver e historial. */
export function ReservaItem({ reserva, sesion, esPersonal, onActualizada, onSesionExpirada }: ReservaItemProps) {
  const [modo, setModo] = useState<Modo>("ninguno");
  const [fechaInicio, setFechaInicio] = useState(reserva.fechaInicio);
  const [fechaFin, setFechaFin] = useState(reserva.fechaFin);
  const [motivo, setMotivo] = useState("");
  const [trabajando, setTrabajando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [historial, setHistorial] = useState<CambioReserva[] | null>(null);
  const [verHistorial, setVerHistorial] = useState(false);

  const estado = ESTADOS_RESERVA[reserva.estado];
  // El personal de sucursal gestiona las reservas con recojo en su sede (el administrador general, todas) y,
  // además, registra la devolución de las que vuelven a su sede.
  const esGeneral = sesion.rol === "ADMINISTRADOR";
  const puedeActuar = !esPersonal || esGeneral || sesion.sucursal === reserva.sucursalRecojo;
  const puedeRegistrarDevolucion = puedeActuar || sesion.sucursal === reserva.sucursalDevolucion;

  async function ejecutar(accion: () => Promise<Reserva>) {
    setTrabajando(true);
    setError(null);
    try {
      const actualizada = await accion();
      onActualizada(actualizada);
      setModo("ninguno");
      setMotivo("");
      if (verHistorial) setHistorial(await consultarHistorialReserva(sesion, reserva.idReserva));
    } catch (cause) {
      if (cause instanceof SesionExpiradaError) {
        onSesionExpirada();
        return;
      }
      setError(cause instanceof Error ? cause.message : "No se pudo completar la acción.");
    } finally {
      setTrabajando(false);
    }
  }

  async function alternarHistorial() {
    const abrir = !verHistorial;
    setVerHistorial(abrir);
    if (!abrir || historial) return;
    try {
      setHistorial(await consultarHistorialReserva(sesion, reserva.idReserva));
    } catch (cause) {
      if (cause instanceof SesionExpiradaError) {
        onSesionExpirada();
        return;
      }
      setError(cause instanceof Error ? cause.message : "No se pudo cargar el historial.");
    }
  }

  return (
    <Card className="rounded-2xl border-0 bg-white shadow-[0_14px_44px_-30px_rgba(24,24,27,0.35)] ring-1 ring-zinc-200/80">
      <CardContent className="space-y-4 p-5">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p className="text-base font-semibold text-zinc-950">{etiquetaCategoria(reserva.categoria)}</p>
            <p className="mt-0.5 text-xs text-zinc-400">Código {reserva.idReserva.slice(0, 8).toUpperCase()}</p>
          </div>
          <span className={`rounded-full px-3 py-1 text-xs font-semibold ring-1 ${estado.clases}`}>
            {estado.etiqueta}
          </span>
        </div>

        <dl className="grid gap-2 text-sm text-zinc-600 sm:grid-cols-2">
          <div className="flex items-start gap-2">
            <CalendarDays className="mt-0.5 size-4 shrink-0 text-zinc-400" aria-hidden="true" />
            <div>
              <dt className="sr-only">Fechas</dt>
              <dd>
                {formatearFecha(reserva.fechaInicio)} → {formatearFecha(reserva.fechaFin)}
              </dd>
            </div>
          </div>
          <div className="flex items-start gap-2">
            <MapPin className="mt-0.5 size-4 shrink-0 text-zinc-400" aria-hidden="true" />
            <div>
              <dt className="sr-only">Sucursales</dt>
              <dd>
                Recojo: {etiquetaSucursal(reserva.sucursalRecojo)}
                {reserva.sucursalDevolucion !== reserva.sucursalRecojo && (
                  <span className="block text-zinc-500">Devolución: {etiquetaSucursal(reserva.sucursalDevolucion)}</span>
                )}
              </dd>
            </div>
          </div>
          {esPersonal && reserva.clienteNombre && (
            <div className="flex items-start gap-2 sm:col-span-2">
              <UserRound className="mt-0.5 size-4 shrink-0 text-zinc-400" aria-hidden="true" />
              <div>
                <dt className="sr-only">Cliente</dt>
                <dd>
                  <span className="font-medium text-zinc-800">{reserva.clienteNombre}</span>
                  {reserva.clienteDocumento && <span className="text-zinc-500"> · {reserva.clienteDocumento}</span>}
                  {reserva.clienteTelefono && <span className="text-zinc-500"> · Tel. {reserva.clienteTelefono}</span>}
                </dd>
              </div>
            </div>
          )}
          {reserva.placaAsignada && (
            <div className="flex items-start gap-2">
              <CarFront className="mt-0.5 size-4 shrink-0 text-zinc-400" aria-hidden="true" />
              <div>
                <dt className="sr-only">Vehículo</dt>
                <dd>Vehículo: placa {reserva.placaAsignada}</dd>
              </div>
            </div>
          )}
        </dl>

        {esPersonal && !puedeActuar && (
          <p className="rounded-xl bg-zinc-50 px-3 py-2 text-xs leading-5 text-zinc-500">
            {reserva.estado === "EN_CURSO"
              ? "Sale de otra sucursal: aquí solo puedes registrar su devolución."
              : "Sale de otra sucursal: la gestiona su sucursal de recojo."}
          </p>
        )}

        {puedeRegistrarDevolucion && (reserva.estado === "CONFIRMADA" || reserva.estado === "EN_CURSO") && modo === "ninguno" && (
          <div className="flex flex-wrap gap-2 border-t border-zinc-100 pt-4">
            {reserva.estado === "CONFIRMADA" && puedeActuar && (
              <>
                {esPersonal && (
                  <Button
                    type="button"
                    disabled={trabajando}
                    onClick={() => ejecutar(() => modificarReserva(sesion, reserva.idReserva, { estado: "EN_CURSO" }))}
                    className="h-9 rounded-xl bg-[#ffd400] px-4 text-sm font-semibold text-zinc-950 hover:bg-[#f2c900]"
                  >
                    {trabajando && <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />}
                    Entregar vehículo
                  </Button>
                )}
                <Button type="button" variant="outline" disabled={trabajando} onClick={() => setModo("modificar")} className="h-9 rounded-xl px-4 text-sm">
                  Cambiar fechas
                </Button>
                <Button type="button" variant="outline" disabled={trabajando} onClick={() => setModo("cancelar")} className="h-9 rounded-xl border-red-200 px-4 text-sm text-red-700 hover:bg-red-50">
                  Cancelar reserva
                </Button>
              </>
            )}
            {reserva.estado === "EN_CURSO" && esPersonal && (
              <Button
                type="button"
                disabled={trabajando}
                onClick={() => ejecutar(() => modificarReserva(sesion, reserva.idReserva, { estado: "FINALIZADA" }))}
                className="h-9 rounded-xl bg-[#ffd400] px-4 text-sm font-semibold text-zinc-950 hover:bg-[#f2c900]"
              >
                {trabajando && <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />}
                Registrar devolución
              </Button>
            )}
          </div>
        )}

        {modo === "modificar" && (
          <div className="space-y-3 rounded-xl bg-zinc-50/80 p-4 ring-1 ring-zinc-200/70">
            <div className="grid gap-3 sm:grid-cols-2">
              <div className="space-y-1">
                <label htmlFor={`ini-${reserva.idReserva}`} className="text-xs font-semibold text-zinc-700">
                  Desde
                </label>
                <input
                  id={`ini-${reserva.idReserva}`}
                  type="date"
                  min={hoyEnLima()}
                  value={fechaInicio}
                  onChange={(e) => {
                    setFechaInicio(e.target.value);
                    if (fechaFin < e.target.value) setFechaFin(e.target.value);
                  }}
                  className={CAMPO_FECHA}
                />
              </div>
              <div className="space-y-1">
                <label htmlFor={`fin-${reserva.idReserva}`} className="text-xs font-semibold text-zinc-700">
                  Hasta
                </label>
                <input
                  id={`fin-${reserva.idReserva}`}
                  type="date"
                  min={fechaInicio}
                  value={fechaFin}
                  onChange={(e) => setFechaFin(e.target.value)}
                  className={CAMPO_FECHA}
                />
              </div>
            </div>
            <div className="flex gap-2">
              <Button
                type="button"
                disabled={trabajando}
                onClick={() => ejecutar(() => modificarReserva(sesion, reserva.idReserva, { fechaInicio, fechaFin }))}
                className="h-9 rounded-xl bg-[#ffd400] px-4 text-sm font-semibold text-zinc-950 hover:bg-[#f2c900]"
              >
                {trabajando && <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />}
                Guardar cambios
              </Button>
              <Button type="button" variant="outline" disabled={trabajando} onClick={() => setModo("ninguno")} className="h-9 rounded-xl px-4 text-sm">
                Volver
              </Button>
            </div>
          </div>
        )}

        {modo === "cancelar" && (
          <div className="space-y-3 rounded-xl bg-red-50/70 p-4 ring-1 ring-red-200">
            <p className="text-sm font-medium text-red-800">¿Cancelar esta reserva? El vehículo quedará libre para otros clientes.</p>
            <input
              type="text"
              value={motivo}
              onChange={(e) => setMotivo(e.target.value)}
              maxLength={200}
              placeholder="Motivo (opcional)"
              aria-label="Motivo de la cancelación"
              className={CAMPO_FECHA}
            />
            <div className="flex gap-2">
              <Button
                type="button"
                disabled={trabajando}
                onClick={() => ejecutar(() => cancelarReserva(sesion, reserva.idReserva, motivo))}
                className="h-9 rounded-xl bg-red-600 px-4 text-sm font-semibold text-white hover:bg-red-700"
              >
                {trabajando && <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />}
                Sí, cancelar
              </Button>
              <Button type="button" variant="outline" disabled={trabajando} onClick={() => setModo("ninguno")} className="h-9 rounded-xl px-4 text-sm">
                No, volver
              </Button>
            </div>
          </div>
        )}

        {error && (
          <div role="alert" className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm leading-5 text-red-700">
            {error}
          </div>
        )}

        {esPersonal && (
          <div className="border-t border-zinc-100 pt-3">
            <button
              type="button"
              onClick={alternarHistorial}
              aria-expanded={verHistorial}
              className="inline-flex items-center gap-1.5 text-xs font-semibold text-zinc-500 transition-colors hover:text-zinc-900"
            >
              <History className="size-3.5" aria-hidden="true" />
              {verHistorial ? "Ocultar historial" : "Ver historial de cambios"}
            </button>
            {verHistorial && (
              <ol className="mt-3 space-y-2">
                {historial === null && <li className="text-xs text-zinc-400">Cargando...</li>}
                {historial?.map((c, i) => (
                  <li key={`${c.fecha}-${i}`} className="rounded-lg bg-zinc-50 px-3 py-2 text-xs leading-5 text-zinc-600">
                    <span className="font-semibold text-zinc-800">{c.accion}</span> · {c.usuario} ({c.rol}) ·{" "}
                    {formatearFechaHora(c.fecha)}
                    {c.detalle && <span className="block text-zinc-500">{c.detalle}</span>}
                  </li>
                ))}
              </ol>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
