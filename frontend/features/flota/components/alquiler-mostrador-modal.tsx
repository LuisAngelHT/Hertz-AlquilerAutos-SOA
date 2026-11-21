"use client";

import { useState } from "react";
import Link from "next/link";
import { CalendarDays, CarFront, CircleCheck, FileSignature, LoaderCircle, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { VentanaModal } from "@/components/ventana-modal";
import { buscarCliente } from "@/features/clientes/lib/cliente-api";
import { TIPOS_DOCUMENTO, type Cliente, type TipoDocumento } from "@/features/clientes/types/cliente";
import { RevisionVehiculoForm } from "@/features/contratos/components/revision-vehiculo-form";
import { tipoCarroceria } from "@/features/contratos/components/vehiculo-diagrama";
import {
  errorRevision,
  revisionAPayload,
  revisionVacia,
  type RevisionForm,
} from "@/features/contratos/lib/revision";
import { SUCURSALES, etiquetaCategoria, etiquetaSucursal } from "@/features/flota/lib/constants";
import type { Vehiculo } from "@/features/flota/lib/flota-api";
import type { Sesion } from "@/features/flota/lib/session";
import { formatearFecha, hoyEnLima } from "@/features/reservas/lib/constants";
import {
  cancelarReserva,
  consultarReserva,
  crearReserva,
  modificarReserva,
  SesionExpiradaError,
  type Reserva,
} from "@/features/reservas/lib/reservas-api";

const CAMPO =
  "h-11 w-full rounded-xl border border-zinc-200 bg-zinc-50/60 px-3 text-sm text-zinc-900 outline-none transition-colors focus-visible:border-amber-400 focus-visible:ring-3 focus-visible:ring-amber-300/40 disabled:opacity-50";

/** Maximo de dias de un alquiler (igual que RESERVA_MAX_DIAS del servicio de reservas). */
const MAX_DIAS = 60;

function sumarDias(iso: string, dias: number): string {
  const [anio, mes, dia] = iso.split("-").map(Number);
  return new Date(Date.UTC(anio, mes - 1, dia + dias)).toISOString().slice(0, 10);
}

// datos: cliente y periodo · revision: revisión del vehículo y firma · pendiente: la reserva quedó creada pero la
// entrega no se completó · listo: alquiler registrado.
type Fase = "datos" | "revision" | "procesando" | "pendiente" | "listo";

interface AlquilerMostradorModalProps {
  vehiculo: Vehiculo;
  sesion: Sesion;
  onCerrar: () => void;
  /** Se llama cuando el alquiler quedo registrado (el padre vuelve a leer el vehiculo, que ya figura ALQUILADO). */
  onAlquilado: () => void;
  onSesionExpirada: () => void;
}

/**
 * Alquiler en mostrador: el cliente se presenta y se lleva el vehiculo ahora. No es una reserva a futuro: se crea la
 * reserva de ESTE vehiculo para hoy y se hace la entrega en el acto. La entrega lleva la revision del vehiculo (como
 * sale y que danos ya tiene) y la confirmacion de que el contrato impreso fue firmado; solo asi se crea el contrato.
 */
export function AlquilerMostradorModal({
  vehiculo,
  sesion,
  onCerrar,
  onAlquilado,
  onSesionExpirada,
}: AlquilerMostradorModalProps) {
  const hoy = hoyEnLima();

  const [tipoDocumento, setTipoDocumento] = useState<TipoDocumento>("DNI");
  const [numeroDocumento, setNumeroDocumento] = useState("");
  const [cliente, setCliente] = useState<Cliente | null>(null);
  const [buscando, setBuscando] = useState(false);
  const [fechaFin, setFechaFin] = useState(sumarDias(hoy, 1));
  const [devolucion, setDevolucion] = useState<string>(vehiculo.sucursal);

  const [revision, setRevision] = useState<RevisionForm>(revisionVacia());
  const [firmado, setFirmado] = useState(false);

  const [fase, setFase] = useState<Fase>("datos");
  const [reserva, setReserva] = useState<Reserva | null>(null);
  const [error, setError] = useState<string | null>(null);

  const ocupado = fase === "procesando";
  const licenciaVencePronto = cliente?.vencimientoLicencia != null && cliente.vencimientoLicencia < fechaFin;
  const sinLicencia = cliente !== null && (!cliente.licenciaVigente || licenciaVencePronto);
  const nombreCliente = cliente ? `${cliente.nombres} ${cliente.apellidos}` : "";

  function manejarError(cause: unknown, porDefecto: string) {
    if (cause instanceof SesionExpiradaError) {
      onSesionExpirada();
      return;
    }
    setError(cause instanceof Error ? cause.message : porDefecto);
  }

  async function buscarPorDocumento() {
    setBuscando(true);
    setError(null);
    setCliente(null);
    try {
      setCliente(await buscarCliente(sesion.token, tipoDocumento, numeroDocumento));
    } catch (cause) {
      manejarError(cause, "No se pudo buscar al cliente.");
    } finally {
      setBuscando(false);
    }
  }

  function continuar() {
    if (!cliente) {
      setError("Primero busca al cliente por su documento.");
      return;
    }
    if (fechaFin < hoy) {
      setError("La fecha de devolución no puede ser anterior a hoy.");
      return;
    }
    setError(null);
    setFase("revision");
  }

  /** Entrega el vehiculo de una reserva ya creada (la primera vez o al reintentar). */
  async function entregar(actual: Reserva) {
    try {
      await modificarReserva(sesion, actual.idReserva, {
        estado: "EN_CURSO",
        entrega: revisionAPayload(revision, false),
        contratoFirmado: true,
      });
      setFase("listo");
      onAlquilado();
    } catch (cause) {
      if (cause instanceof SesionExpiradaError) {
        onSesionExpirada();
        return;
      }
      const texto = cause instanceof Error ? cause.message : "No se pudo registrar la entrega del vehículo.";
      // Se consulta el estado real: si ya está en curso, solo falta crear el contrato (ya no se puede cancelar).
      let estadoReal: Reserva["estado"] = "CONFIRMADA";
      try {
        estadoReal = (await consultarReserva(sesion, actual.idReserva)).estado;
      } catch {
        /* se asume que sigue confirmada */
      }
      setReserva({ ...actual, estado: estadoReal });
      setError(
        estadoReal === "EN_CURSO"
          ? `El vehículo ya figura entregado, pero el contrato no se pudo crear. ${texto}`
          : texto,
      );
      setFase("pendiente");
    }
  }

  async function registrar() {
    if (!cliente) return;
    const problema = errorRevision(revision, { esDevolucion: false });
    if (problema) {
      setError(problema);
      return;
    }
    if (!firmado) {
      setError("Confirma que el cliente firmó el contrato impreso.");
      return;
    }
    setFase("procesando");
    setError(null);
    let creada: Reserva;
    try {
      creada = await crearReserva(sesion, {
        idCliente: cliente.idCliente,
        categoria: vehiculo.categoria,
        placa: vehiculo.placa,
        sucursalRecojo: vehiculo.sucursal,
        sucursalDevolucion: devolucion,
        fechaInicio: hoy,
        fechaFin,
      });
    } catch (cause) {
      manejarError(cause, "No se pudo registrar el alquiler.");
      setFase("revision");
      return;
    }
    setReserva(creada);
    await entregar(creada);
  }

  async function cancelarAlquiler() {
    if (!reserva) return onCerrar();
    setFase("procesando");
    setError(null);
    try {
      await cancelarReserva(sesion, reserva.idReserva, "Alquiler en mostrador no completado");
      onCerrar();
    } catch (cause) {
      manejarError(cause, "No se pudo cancelar la reserva.");
      setFase("pendiente");
    }
  }

  const subtitulo = (
    <span className="flex items-center gap-1.5">
      <CarFront className="size-4 text-amber-600" aria-hidden="true" />
      {vehiculo.marca} {vehiculo.modelo} · <span className="font-mono">{vehiculo.placa}</span> ·{" "}
      {etiquetaCategoria(vehiculo.categoria)}
    </span>
  );

  return (
    <VentanaModal
      titulo="Alquilar vehículo"
      subtitulo={subtitulo}
      onCerrar={onCerrar}
      ocupado={ocupado}
      ancho={fase === "datos" ? "max-w-lg" : "max-w-4xl"}
    >
      {fase === "listo" && reserva ? (
        <div className="mt-6 space-y-4" aria-live="polite">
          <div className="flex items-start gap-3 rounded-2xl bg-emerald-50 p-4 ring-1 ring-emerald-200">
            <CircleCheck className="mt-0.5 size-6 shrink-0 text-emerald-700" aria-hidden="true" />
            <div className="text-sm text-emerald-900">
              <p className="font-semibold">Alquiler registrado y contrato creado</p>
              <p className="mt-1">
                {nombreCliente} se lleva el {vehiculo.marca} {vehiculo.modelo} ({vehiculo.placa}) hasta el{" "}
                {formatearFecha(fechaFin)}, con devolución en {etiquetaSucursal(devolucion)}. La revisión de salida
                quedó registrada.
              </p>
            </div>
          </div>
          <div className="flex items-start gap-3 rounded-2xl bg-amber-50 p-4 ring-1 ring-amber-200">
            <FileSignature className="mt-0.5 size-5 shrink-0 text-amber-700" aria-hidden="true" />
            <p className="text-sm text-amber-900">
              Siguiente paso: escanea el contrato firmado y súbelo en <strong>Contratos</strong> (busca la placa{" "}
              {vehiculo.placa}).
            </p>
          </div>
          <div className="flex flex-wrap justify-end gap-2">
            <Button type="button" variant="outline" onClick={onCerrar} className="h-10 rounded-xl px-4">
              Cerrar
            </Button>
            <Link
              href="/contratos"
              className="inline-flex h-10 items-center justify-center rounded-xl bg-[#ffd400] px-4 text-sm font-bold text-zinc-950 transition-colors hover:bg-[#f2c900] focus-visible:outline focus-visible:outline-2 focus-visible:outline-amber-500"
            >
              Ir a Contratos
            </Link>
          </div>
        </div>
      ) : fase === "datos" ? (
        <div className="mt-5 space-y-5">
          <fieldset className="space-y-3 rounded-2xl bg-zinc-50/70 p-4 ring-1 ring-zinc-200/70">
            <legend className="px-1 text-sm font-semibold text-zinc-800">Cliente</legend>
            <div className="grid gap-3 sm:grid-cols-[7rem_1fr_auto] sm:items-end">
              <label className="space-y-1.5 text-xs font-medium text-zinc-600">
                Documento
                <select
                  value={tipoDocumento}
                  onChange={(e) => {
                    setTipoDocumento(e.target.value as TipoDocumento);
                    setCliente(null);
                  }}
                  className={CAMPO}
                >
                  {TIPOS_DOCUMENTO.map((tipo) => (
                    <option key={tipo} value={tipo}>
                      {tipo}
                    </option>
                  ))}
                </select>
              </label>
              <label className="space-y-1.5 text-xs font-medium text-zinc-600">
                Número
                <input
                  value={numeroDocumento}
                  onChange={(e) => {
                    setNumeroDocumento(e.target.value);
                    setCliente(null);
                  }}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" && numeroDocumento.trim() !== "" && !buscando) {
                      e.preventDefault();
                      void buscarPorDocumento();
                    }
                  }}
                  placeholder="Ej. 45678912"
                  autoFocus
                  autoComplete="off"
                  className={CAMPO}
                />
              </label>
              <Button
                type="button"
                onClick={() => void buscarPorDocumento()}
                disabled={buscando || numeroDocumento.trim() === ""}
                className="h-11 rounded-xl bg-zinc-950 px-4 text-white hover:bg-zinc-800"
              >
                {buscando ? <LoaderCircle className="animate-spin" aria-hidden="true" /> : <Search aria-hidden="true" />}
                Buscar
              </Button>
            </div>
            {cliente && (
              <div className="rounded-xl bg-white px-3 py-2.5 text-sm ring-1 ring-emerald-200">
                <p className="font-semibold text-zinc-900">{nombreCliente}</p>
                <p className="text-xs text-zinc-500">
                  {cliente.tipoDocumento} {cliente.numeroDocumento}
                  {cliente.telefono ? ` · Tel. ${cliente.telefono}` : ""}
                  {cliente.vencimientoLicencia ? ` · Licencia hasta ${formatearFecha(cliente.vencimientoLicencia)}` : ""}
                </p>
                {sinLicencia && (
                  <p role="alert" className="mt-1.5 text-xs font-medium text-red-700">
                    {cliente.licenciaVigente
                      ? "La licencia vence antes de la fecha de devolución elegida."
                      : "La licencia de conducir no está vigente: no se puede alquilar."}
                  </p>
                )}
              </div>
            )}
            {!cliente && (
              <p className="text-xs text-zinc-500">
                ¿El cliente no está registrado?{" "}
                <Link href="/clientes" className="font-semibold text-amber-700 underline underline-offset-2">
                  Regístralo primero en Clientes
                </Link>
                .
              </p>
            )}
          </fieldset>

          <fieldset className="space-y-3 rounded-2xl bg-zinc-50/70 p-4 ring-1 ring-zinc-200/70">
            <legend className="px-1 text-sm font-semibold text-zinc-800">Periodo y devolución</legend>
            <p className="flex items-center gap-2 text-sm text-zinc-700">
              <CalendarDays className="size-4 text-zinc-400" aria-hidden="true" />
              Desde <strong>hoy, {formatearFecha(hoy)}</strong>
            </p>
            <div className="grid gap-3 sm:grid-cols-2">
              <label className="space-y-1.5 text-xs font-medium text-zinc-600">
                Devuelve el
                <input
                  type="date"
                  value={fechaFin}
                  min={hoy}
                  max={sumarDias(hoy, MAX_DIAS)}
                  onChange={(e) => setFechaFin(e.target.value)}
                  className={CAMPO}
                />
              </label>
              <label className="space-y-1.5 text-xs font-medium text-zinc-600">
                Devuelve en
                <select value={devolucion} onChange={(e) => setDevolucion(e.target.value)} className={CAMPO}>
                  {SUCURSALES.map((sede) => (
                    <option key={sede.value} value={sede.value}>
                      {sede.label}
                    </option>
                  ))}
                </select>
              </label>
            </div>
            <div className="flex flex-wrap gap-2">
              {[
                { texto: "Mañana", dias: 1 },
                { texto: "3 días", dias: 3 },
                { texto: "1 semana", dias: 7 },
                { texto: "2 semanas", dias: 14 },
              ].map((atajo) => (
                <button
                  key={atajo.dias}
                  type="button"
                  onClick={() => setFechaFin(sumarDias(hoy, atajo.dias))}
                  className={`rounded-full px-3 py-1 text-xs font-semibold ring-1 transition-colors ${
                    fechaFin === sumarDias(hoy, atajo.dias)
                      ? "bg-[#fff4b8] text-zinc-950 ring-amber-400"
                      : "bg-white text-zinc-600 ring-zinc-200 hover:bg-zinc-50"
                  }`}
                >
                  {atajo.texto}
                </button>
              ))}
            </div>
          </fieldset>

          {error && (
            <div role="alert" className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm leading-5 text-red-700">
              {error}
            </div>
          )}

          <div className="flex flex-wrap items-center justify-between gap-2">
            <p className="text-xs text-zinc-500">Paso 1 de 2 · Después: revisión del vehículo y firma</p>
            <div className="flex gap-2">
              <Button type="button" variant="outline" onClick={onCerrar} className="h-10 rounded-xl px-4">
                Cancelar
              </Button>
              <Button
                type="button"
                onClick={continuar}
                disabled={!cliente || sinLicencia}
                className="h-10 rounded-xl bg-[#ffd400] px-4 font-bold text-zinc-950 hover:bg-[#f2c900]"
              >
                Continuar
              </Button>
            </div>
          </div>
        </div>
      ) : (
        <div className="mt-5 space-y-5">
          <dl className="grid gap-x-4 gap-y-1 rounded-2xl bg-zinc-50/70 p-4 text-sm ring-1 ring-zinc-200/70 sm:grid-cols-2">
            <div>
              <dt className="text-xs text-zinc-400">Cliente</dt>
              <dd className="font-medium text-zinc-800">
                {nombreCliente}
                <span className="font-normal text-zinc-500">
                  {" · "}
                  {cliente?.tipoDocumento} {cliente?.numeroDocumento}
                </span>
              </dd>
            </div>
            <div>
              <dt className="text-xs text-zinc-400">Periodo</dt>
              <dd className="font-medium text-zinc-800">
                {formatearFecha(hoy)} → {formatearFecha(fechaFin)} · devuelve en {etiquetaSucursal(devolucion)}
              </dd>
            </div>
          </dl>

          <p className="text-sm leading-6 text-zinc-600">
            <strong className="text-zinc-800">1.</strong> Revisa el vehículo con el cliente. <strong className="text-zinc-800">2.</strong>{" "}
            Imprime el contrato y haz que lo firme. <strong className="text-zinc-800">3.</strong> Confirma aquí para entregar.
          </p>

          <RevisionVehiculoForm
            modo="ENTREGA"
            valor={revision}
            onCambio={setRevision}
            tipo={tipoCarroceria(vehiculo.categoria)}
            deshabilitado={fase !== "revision"}
          />

          <label className="flex items-start gap-3 rounded-xl bg-amber-50 p-3 text-sm text-amber-950 ring-1 ring-amber-200">
            <input
              type="checkbox"
              checked={firmado}
              disabled={fase !== "revision"}
              onChange={(e) => setFirmado(e.target.checked)}
              className="mt-0.5 size-4 accent-amber-500"
            />
            <span>
              El cliente <strong>firmó el contrato impreso</strong> y revisó el estado del vehículo conmigo. Sin esto el
              vehículo no sale.
            </span>
          </label>

          {error && (
            <div role="alert" className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm leading-5 text-red-700">
              {error}
            </div>
          )}

          {fase === "pendiente" && (
            <div className="rounded-xl bg-amber-50 p-3 text-sm leading-5 text-amber-900 ring-1 ring-amber-200">
              {reserva?.estado === "EN_CURSO"
                ? "Puedes reintentar crear el contrato: la revisión y la firma que registraste se reenvían."
                : "La reserva quedó creada, pero no se pudo registrar la entrega. Puedes reintentar la entrega o cancelar este alquiler (se libera el vehículo)."}
            </div>
          )}

          <div className="flex flex-wrap items-center justify-between gap-2">
            <p className="text-xs text-zinc-500">Paso 2 de 2</p>
            <div className="flex flex-wrap justify-end gap-2">
              {fase === "pendiente" ? (
                <>
                  {reserva?.estado === "EN_CURSO" ? (
                    <Button type="button" variant="outline" onClick={onCerrar} className="h-10 rounded-xl px-4">
                      Cerrar
                    </Button>
                  ) : (
                    <Button type="button" variant="outline" onClick={() => void cancelarAlquiler()} className="h-10 rounded-xl px-4">
                      Cancelar alquiler
                    </Button>
                  )}
                  <Button
                    type="button"
                    onClick={() => {
                      if (!reserva) return;
                      setFase("procesando");
                      setError(null);
                      void entregar(reserva);
                    }}
                    className="h-10 rounded-xl bg-[#ffd400] px-4 font-bold text-zinc-950 hover:bg-[#f2c900]"
                  >
                    {reserva?.estado === "EN_CURSO" ? "Reintentar crear el contrato" : "Reintentar entrega"}
                  </Button>
                </>
              ) : (
                <>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => {
                      setError(null);
                      setFase("datos");
                    }}
                    disabled={ocupado}
                    className="h-10 rounded-xl px-4"
                  >
                    Volver
                  </Button>
                  <Button
                    type="button"
                    onClick={() => void registrar()}
                    disabled={ocupado}
                    className="h-10 rounded-xl bg-[#ffd400] px-4 font-bold text-zinc-950 hover:bg-[#f2c900]"
                  >
                    {ocupado && <LoaderCircle className="animate-spin" aria-hidden="true" />}
                    {ocupado ? "Registrando..." : "Entregar y registrar alquiler"}
                  </Button>
                </>
              )}
            </div>
          </div>
        </div>
      )}
    </VentanaModal>
  );
}
