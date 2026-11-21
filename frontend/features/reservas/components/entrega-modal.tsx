"use client";

import { useState } from "react";
import Link from "next/link";
import { CircleCheck, FileSignature, LoaderCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { VentanaModal } from "@/components/ventana-modal";
import { RevisionVehiculoForm } from "@/features/contratos/components/revision-vehiculo-form";
import { tipoCarroceria } from "@/features/contratos/components/vehiculo-diagrama";
import {
  errorRevision,
  revisionAPayload,
  revisionVacia,
  type RevisionForm,
} from "@/features/contratos/lib/revision";
import { etiquetaCategoria, etiquetaSucursal } from "@/features/flota/lib/constants";
import type { Sesion } from "@/features/flota/lib/session";
import { formatearFecha } from "@/features/reservas/lib/constants";
import {
  consultarReserva,
  modificarReserva,
  SesionExpiradaError,
  type Reserva,
} from "@/features/reservas/lib/reservas-api";

type Fase = "formulario" | "procesando" | "pendiente" | "listo";

interface EntregaModalProps {
  reserva: Reserva;
  sesion: Sesion;
  onCerrar: () => void;
  /** Se llama con la reserva ya actualizada (para que la lista muestre el estado nuevo). */
  onCompletada: (reserva: Reserva) => void;
  onSesionExpirada: () => void;
}

/**
 * Entrega del vehículo: el acto serio. Se revisa el vehículo con el cliente (kilometraje, combustible y los daños
 * que ya tenga, para que después no cargue con ellos), se confirma que el contrato impreso fue firmado y recién
 * ahí el vehículo sale y se crea el contrato.
 */
export function EntregaModal({ reserva, sesion, onCerrar, onCompletada, onSesionExpirada }: EntregaModalProps) {
  const [revision, setRevision] = useState<RevisionForm>(revisionVacia());
  const [firmado, setFirmado] = useState(false);
  const [fase, setFase] = useState<Fase>("formulario");
  const [error, setError] = useState<string | null>(null);

  const ocupado = fase === "procesando";
  const bloqueado = fase !== "formulario";

  async function entregar() {
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
    try {
      const actualizada = await modificarReserva(sesion, reserva.idReserva, {
        estado: "EN_CURSO",
        entrega: revisionAPayload(revision, false),
        contratoFirmado: true,
      });
      setFase("listo");
      onCompletada(actualizada);
    } catch (cause) {
      if (cause instanceof SesionExpiradaError) {
        onSesionExpirada();
        return;
      }
      const texto = cause instanceof Error ? cause.message : "No se pudo registrar la entrega.";
      // El vehículo pudo salir y fallar solo la creación del contrato: se consulta el estado real.
      try {
        const actual = await consultarReserva(sesion, reserva.idReserva);
        if (actual.estado === "EN_CURSO") {
          onCompletada(actual);
          setError(`La reserva ya figura en curso, pero el contrato no se pudo crear. ${texto}`);
          setFase("pendiente");
          return;
        }
      } catch {
        /* si tampoco se puede consultar, se deja el mensaje original */
      }
      setError(texto);
      setFase("formulario");
    }
  }

  return (
    <VentanaModal
      titulo="Entregar vehículo"
      subtitulo={
        <>
          {etiquetaCategoria(reserva.categoria)}
          {reserva.placaAsignada ? (
            <>
              {" · "}
              <span className="font-mono">{reserva.placaAsignada}</span>
            </>
          ) : null}
        </>
      }
      onCerrar={onCerrar}
      ocupado={ocupado}
      ancho="max-w-4xl"
    >
      {fase === "listo" ? (
        <div className="mt-6 space-y-4" aria-live="polite">
          <div className="flex items-start gap-3 rounded-2xl bg-emerald-50 p-4 ring-1 ring-emerald-200">
            <CircleCheck className="mt-0.5 size-6 shrink-0 text-emerald-700" aria-hidden="true" />
            <div className="text-sm text-emerald-900">
              <p className="font-semibold">Vehículo entregado y contrato creado</p>
              <p className="mt-1">
                La revisión de salida quedó registrada: si al devolverlo aparecen daños nuevos, se sabrá que no estaban.
              </p>
            </div>
          </div>
          <div className="flex items-start gap-3 rounded-2xl bg-amber-50 p-4 ring-1 ring-amber-200">
            <FileSignature className="mt-0.5 size-5 shrink-0 text-amber-700" aria-hidden="true" />
            <p className="text-sm text-amber-900">
              Siguiente paso: escanea el contrato firmado y súbelo en <strong>Contratos</strong>
              {reserva.placaAsignada ? <> (busca la placa {reserva.placaAsignada})</> : null}.
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
      ) : (
        <div className="mt-5 space-y-5">
          <dl className="grid gap-x-4 gap-y-1 rounded-2xl bg-zinc-50/70 p-4 text-sm ring-1 ring-zinc-200/70 sm:grid-cols-2">
            <div>
              <dt className="text-xs text-zinc-400">Cliente</dt>
              <dd className="font-medium text-zinc-800">
                {reserva.clienteNombre ?? "Cliente sin datos"}
                {reserva.clienteDocumento ? (
                  <span className="font-normal text-zinc-500"> · {reserva.clienteDocumento}</span>
                ) : null}
              </dd>
            </div>
            <div>
              <dt className="text-xs text-zinc-400">Periodo</dt>
              <dd className="font-medium text-zinc-800">
                {formatearFecha(reserva.fechaInicio)} → {formatearFecha(reserva.fechaFin)}
              </dd>
            </div>
            <div className="sm:col-span-2">
              <dt className="text-xs text-zinc-400">Recojo y devolución</dt>
              <dd className="font-medium text-zinc-800">
                {etiquetaSucursal(reserva.sucursalRecojo)} → {etiquetaSucursal(reserva.sucursalDevolucion)}
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
            tipo={tipoCarroceria(reserva.categoria)}
            deshabilitado={bloqueado}
          />

          <label className="flex items-start gap-3 rounded-xl bg-amber-50 p-3 text-sm text-amber-950 ring-1 ring-amber-200">
            <input
              type="checkbox"
              checked={firmado}
              disabled={bloqueado}
              onChange={(e) => setFirmado(e.target.checked)}
              className="mt-0.5 size-4 accent-amber-500"
            />
            <span>
              El cliente <strong>firmó el contrato impreso</strong> y revisó el estado del vehículo conmigo. Sin esto
              el vehículo no sale.
            </span>
          </label>

          {error && (
            <div role="alert" className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm leading-5 text-red-700">
              {error}
            </div>
          )}

          <div className="flex flex-wrap justify-end gap-2">
            <Button type="button" variant="outline" onClick={onCerrar} disabled={ocupado} className="h-10 rounded-xl px-4">
              {fase === "pendiente" ? "Cerrar" : "Cancelar"}
            </Button>
            <Button
              type="button"
              onClick={() => void entregar()}
              disabled={ocupado}
              className="h-10 rounded-xl bg-[#ffd400] px-4 font-bold text-zinc-950 hover:bg-[#f2c900]"
            >
              {ocupado && <LoaderCircle className="animate-spin" aria-hidden="true" />}
              {ocupado ? "Registrando..." : fase === "pendiente" ? "Reintentar crear el contrato" : "Entregar vehículo"}
            </Button>
          </div>
        </div>
      )}
    </VentanaModal>
  );
}
