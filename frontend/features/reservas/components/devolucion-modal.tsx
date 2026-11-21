"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { CircleAlert, CircleCheck, LoaderCircle, Wrench } from "lucide-react";
import { Button } from "@/components/ui/button";
import { VentanaModal } from "@/components/ventana-modal";
import { RevisionVehiculoForm } from "@/features/contratos/components/revision-vehiculo-form";
import { tipoCarroceria } from "@/features/contratos/components/vehiculo-diagrama";
import { obtenerContratoPorReserva } from "@/features/contratos/lib/contratos-api";
import {
  cuartos,
  errorRevision,
  etiquetaCombustible,
  etiquetaGravedad,
  etiquetaZona,
  formatoKm,
  formatoSoles,
  revisionAPayload,
  revisionVacia,
  totalACargo,
  type RevisionForm,
} from "@/features/contratos/lib/revision";
import type { Contrato } from "@/features/contratos/types/contrato";
import { etiquetaCategoria } from "@/features/flota/lib/constants";
import type { Sesion } from "@/features/flota/lib/session";
import {
  consultarReserva,
  modificarReserva,
  SesionExpiradaError,
  type Reserva,
} from "@/features/reservas/lib/reservas-api";
import { ApiRequestError } from "@/lib/api-client";

type Fase = "cargando" | "formulario" | "procesando" | "listo";

interface DevolucionModalProps {
  reserva: Reserva;
  sesion: Sesion;
  onCerrar: () => void;
  /** Se llama con la reserva ya actualizada (para que la lista muestre el estado nuevo). */
  onCompletada: (reserva: Reserva) => void;
  onSesionExpirada: () => void;
  /** Si la reserva está en curso pero sin contrato, permite completar primero la entrega (abre esa ventana). */
  onCompletarEntrega?: () => void;
}

interface Resultado {
  recorridos: number | null;
  danosNuevos: number;
  total: number;
  conforme: boolean;
}

/**
 * Devolución del vehículo: se revisa como vuelve y se compara con cómo salió. Los daños que no tenía al salir
 * son del cliente: quedan registrados con su costo estimado. Al confirmar, el contrato se cierra.
 */
export function DevolucionModal({
  reserva,
  sesion,
  onCerrar,
  onCompletada,
  onSesionExpirada,
  onCompletarEntrega,
}: DevolucionModalProps) {
  const [fase, setFase] = useState<Fase>("cargando");
  const [contrato, setContrato] = useState<Contrato | null>(null);
  const [sinContrato, setSinContrato] = useState(false);
  const [revision, setRevision] = useState<RevisionForm>(revisionVacia());
  const [conforme, setConforme] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [resultado, setResultado] = useState<Resultado | null>(null);

  // La revisión de salida del contrato es la referencia contra la que se compara lo que vuelve.
  useEffect(() => {
    let activo = true;
    obtenerContratoPorReserva(sesion.token, reserva.idReserva)
      .then((datos) => {
        if (!activo) return;
        setContrato(datos);
        // El inventario parte de como salió: solo se marca lo que cambió.
        setRevision(revisionVacia(datos.entrega));
        setFase("formulario");
      })
      .catch((cause: unknown) => {
        if (!activo) return;
        if (cause instanceof ApiRequestError && cause.status === 401) {
          onSesionExpirada();
          return;
        }
        if (cause instanceof ApiRequestError && cause.status === 404) {
          setSinContrato(true);
        } else {
          setError(cause instanceof Error ? cause.message : "No se pudo leer el contrato de esta reserva.");
        }
        setFase("formulario");
      });
    return () => {
      activo = false;
    };
  }, [sesion.token, reserva.idReserva, onSesionExpirada]);

  const entrega = contrato?.entrega ?? null;
  const ocupado = fase === "procesando";
  const bloqueado = fase !== "formulario" || sinContrato;
  const total = totalACargo(revision);
  const kmSalida = entrega?.kilometraje ?? null;
  const kmActual = Number(revision.kilometraje);
  const recorridos =
    kmSalida != null && revision.kilometraje.trim() !== "" && Number.isFinite(kmActual) ? kmActual - kmSalida : null;
  const menosCombustible =
    entrega != null && revision.combustible !== "" && cuartos(revision.combustible) < cuartos(entrega.combustible);

  async function devolver() {
    const problema = errorRevision(revision, { esDevolucion: true, kmMinimo: kmSalida });
    if (problema) {
      setError(problema);
      return;
    }
    if (!conforme && revision.observaciones.trim() === "") {
      setError("Si el cliente no está conforme, explica el motivo en las observaciones.");
      return;
    }
    setFase("procesando");
    setError(null);
    try {
      const actualizada = await modificarReserva(sesion, reserva.idReserva, {
        estado: "FINALIZADA",
        devolucion: { ...revisionAPayload(revision, true, entrega), clienteConforme: conforme },
      });
      setResultado({ recorridos, danosNuevos: revision.danos.length, total, conforme });
      setFase("listo");
      onCompletada(actualizada);
    } catch (cause) {
      if (cause instanceof SesionExpiradaError) {
        onSesionExpirada();
        return;
      }
      const texto = cause instanceof Error ? cause.message : "No se pudo registrar la devolución.";
      // Si la reserva llegó a finalizarse pero algo posterior falló, la lista debe mostrar el estado real.
      try {
        const actual = await consultarReserva(sesion, reserva.idReserva);
        if (actual.estado !== reserva.estado) onCompletada(actual);
      } catch {
        /* se deja el mensaje original */
      }
      setError(texto);
      setFase("formulario");
    }
  }

  return (
    <VentanaModal
      titulo="Registrar devolución"
      subtitulo={
        <>
          {etiquetaCategoria(reserva.categoria)}
          {reserva.placaAsignada ? (
            <>
              {" · "}
              <span className="font-mono">{reserva.placaAsignada}</span>
            </>
          ) : null}
          {reserva.clienteNombre ? ` · ${reserva.clienteNombre}` : ""}
        </>
      }
      onCerrar={onCerrar}
      ocupado={ocupado}
      ancho="max-w-4xl"
    >
      {fase === "cargando" && (
        <p className="mt-8 flex items-center justify-center gap-2 pb-4 text-sm text-zinc-500">
          <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
          Cargando la revisión de salida...
        </p>
      )}

      {fase === "listo" && resultado && (
        <div className="mt-6 space-y-4" aria-live="polite">
          <div className="flex items-start gap-3 rounded-2xl bg-emerald-50 p-4 ring-1 ring-emerald-200">
            <CircleCheck className="mt-0.5 size-6 shrink-0 text-emerald-700" aria-hidden="true" />
            <div className="text-sm text-emerald-900">
              <p className="font-semibold">Vehículo recibido y contrato finalizado</p>
              <p className="mt-1">
                {resultado.recorridos != null ? `Recorrió ${formatoKm(resultado.recorridos)}. ` : ""}
                {resultado.danosNuevos === 0
                  ? "Sin daños nuevos."
                  : `${resultado.danosNuevos} daño${resultado.danosNuevos === 1 ? " nuevo" : "s nuevos"} registrado${resultado.danosNuevos === 1 ? "" : "s"}.`}
              </p>
            </div>
          </div>
          {resultado.total > 0 && (
            <div className="rounded-2xl bg-red-50 p-4 text-sm text-red-900 ring-1 ring-red-200">
              <p className="font-semibold">A cargo del cliente: {formatoSoles(resultado.total)}</p>
              <p className="mt-1">
                {resultado.conforme
                  ? "El cliente quedó conforme con lo registrado."
                  : "El cliente NO quedó conforme: el motivo quedó en las observaciones del contrato."}
              </p>
            </div>
          )}
          {resultado.danosNuevos > 0 && (
            <div className="flex items-start gap-3 rounded-2xl bg-amber-50 p-4 ring-1 ring-amber-200">
              <Wrench className="mt-0.5 size-5 shrink-0 text-amber-700" aria-hidden="true" />
              <p className="text-sm text-amber-900">
                Por los daños, se registró automáticamente una <strong>incidencia</strong> y el vehículo pasó a{" "}
                <strong>Mantenimiento</strong>: no estará disponible hasta que se repare y se cierre la incidencia.
              </p>
            </div>
          )}
          <div className="flex flex-wrap justify-end gap-2">
            <Button type="button" variant="outline" onClick={onCerrar} className="h-10 rounded-xl px-4">
              Cerrar
            </Button>
            {resultado.danosNuevos > 0 && (
              <Link
                href="/incidencias"
                className="inline-flex h-10 items-center justify-center rounded-xl bg-[#ffd400] px-4 text-sm font-bold text-zinc-950 transition-colors hover:bg-[#f2c900] focus-visible:outline focus-visible:outline-2 focus-visible:outline-amber-500"
              >
                Ir a Incidencias
              </Link>
            )}
          </div>
        </div>
      )}

      {(fase === "formulario" || fase === "procesando") && (
        <div className="mt-5 space-y-5">
          {sinContrato && (
            <div role="alert" className="flex items-start gap-2 rounded-xl bg-amber-50 p-3 text-sm text-amber-900 ring-1 ring-amber-200">
              <CircleAlert className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
              <div className="space-y-2">
                <p>
                  Esta reserva está en curso pero no tiene contrato (la entrega quedó a medias). Primero hay que
                  completar la entrega; después podrás registrar la devolución.
                </p>
                {onCompletarEntrega && (
                  <Button type="button" size="sm" onClick={onCompletarEntrega} className="rounded-xl bg-[#ffd400] font-bold text-zinc-950 hover:bg-[#f2c900]">
                    Completar la entrega ahora
                  </Button>
                )}
              </div>
            </div>
          )}

          {entrega && (
            <div className="rounded-2xl bg-zinc-50/70 p-4 text-sm ring-1 ring-zinc-200/70">
              <p className="font-semibold text-zinc-800">Así salió el vehículo</p>
              <p className="mt-1 text-zinc-600">
                {formatoKm(entrega.kilometraje)} · {etiquetaCombustible(entrega.combustible)}
              </p>
              {entrega.danos.length > 0 ? (
                <ul className="mt-2 space-y-1 text-xs text-zinc-600">
                  {entrega.danos.map((d, i) => (
                    <li key={i}>
                      • {etiquetaZona(d.zona)} ({etiquetaGravedad(d.gravedad).toLowerCase()}): {d.descripcion}
                    </li>
                  ))}
                  <li className="pt-1 font-medium text-zinc-500">Estos daños ya los tenía: no se le cobran al cliente.</li>
                </ul>
              ) : (
                <p className="mt-1 text-xs text-zinc-500">Salió sin daños.</p>
              )}
            </div>
          )}
          {contrato && !entrega && (
            <div role="alert" className="flex items-start gap-2 rounded-xl bg-amber-50 p-3 text-sm text-amber-900 ring-1 ring-amber-200">
              <CircleAlert className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
              <p>
                <strong>No se puede validar el kilometraje.</strong> Este vehículo se entregó antes de que existiera la
                revisión de salida, así que no hay registro de con cuántos kilómetros ni con qué daños salió. Verifica
                el kilometraje y los daños con el contrato impreso antes de registrar la devolución.
              </p>
            </div>
          )}

          <RevisionVehiculoForm
            modo="DEVOLUCION"
            valor={revision}
            onCambio={setRevision}
            tipo={tipoCarroceria(reserva.categoria)}
            entrega={entrega}
            kmMinimo={kmSalida}
            deshabilitado={bloqueado}
          />

          {(recorridos != null || menosCombustible) && (
            <div className="rounded-xl bg-zinc-50 px-3 py-2 text-sm text-zinc-700 ring-1 ring-zinc-200">
              {recorridos != null && recorridos > 0 && <p>Recorrió {formatoKm(recorridos)} durante el alquiler.</p>}
              {recorridos === 0 && (
                <p className="text-amber-800">
                  Es el mismo kilometraje con el que salió (0 km recorridos). Verifica el odómetro antes de continuar.
                </p>
              )}
              {menosCombustible && entrega && (
                <p className="text-amber-800">
                  Devuelve con menos combustible que al salir ({etiquetaCombustible(entrega.combustible).toLowerCase()} → {etiquetaCombustible(revision.combustible).toLowerCase()}).
                </p>
              )}
            </div>
          )}

          <label className="flex items-start gap-3 rounded-xl bg-zinc-50 p-3 text-sm text-zinc-800 ring-1 ring-zinc-200">
            <input
              type="checkbox"
              checked={conforme}
              disabled={bloqueado}
              onChange={(e) => setConforme(e.target.checked)}
              className="mt-0.5 size-4 accent-amber-500"
            />
            <span>
              El cliente <strong>revisó lo registrado y está conforme</strong>
              {total > 0 ? ` con asumir ${formatoSoles(total)} por los daños nuevos` : ""}. Si no lo está, desmárcalo y
              explica el motivo en las observaciones.
            </span>
          </label>

          {error && (
            <div role="alert" className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm leading-5 text-red-700">
              {error}
            </div>
          )}

          <div className="flex flex-wrap justify-end gap-2">
            <Button type="button" variant="outline" onClick={onCerrar} disabled={ocupado} className="h-10 rounded-xl px-4">
              Cancelar
            </Button>
            <Button
              type="button"
              onClick={() => void devolver()}
              disabled={ocupado || sinContrato}
              className="h-10 rounded-xl bg-[#ffd400] px-4 font-bold text-zinc-950 hover:bg-[#f2c900]"
            >
              {ocupado && <LoaderCircle className="animate-spin" aria-hidden="true" />}
              {ocupado ? "Registrando..." : "Registrar devolución"}
            </Button>
          </div>
        </div>
      )}
    </VentanaModal>
  );
}
