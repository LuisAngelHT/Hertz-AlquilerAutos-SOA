"use client";

import { useState } from "react";
import { CarFront, History, LoaderCircle, MapPin } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { AlquilerMostradorModal } from "@/features/flota/components/alquiler-mostrador-modal";
import { DevolucionModal } from "@/features/reservas/components/devolucion-modal";
import { listarReservas, type Reserva } from "@/features/reservas/lib/reservas-api";
import {
  actualizarEstado,
  consultarHistorial,
  consultarVehiculo,
  SesionExpiradaError,
  type CambioHistorial,
  type EstadoVehiculo,
  type Vehiculo,
} from "@/features/flota/lib/flota-api";
import {
  MOTIVO_OTRO,
  MOTIVOS_CAMBIO,
  etiquetaCategoria,
  etiquetaSucursal,
} from "@/features/flota/lib/constants";
import { puedeCambiarVehiculo, type Sesion } from "@/features/flota/lib/session";

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
  FUERA_DE_SERVICIO: {
    texto: "Fuera de servicio",
    clases: "bg-zinc-100 text-zinc-600 ring-zinc-300",
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
    { destino: "FUERA_DE_SERVICIO", etiqueta: "Dar de baja" },
  ],
  ALQUILADO: [
    { destino: "DISPONIBLE", etiqueta: "Registrar devolución", principal: true },
    { destino: "MANTENIMIENTO", etiqueta: "Enviar a mantenimiento" },
  ],
  MANTENIMIENTO: [
    { destino: "DISPONIBLE", etiqueta: "Marcar disponible", principal: true },
    { destino: "FUERA_DE_SERVICIO", etiqueta: "Dar de baja" },
  ],
  FUERA_DE_SERVICIO: [
    { destino: "DISPONIBLE", etiqueta: "Reactivar vehículo", principal: true },
  ],
};

// Fecha y hora local legible para el historial.
function formatearFecha(iso: string): string {
  const fecha = new Date(iso);
  return Number.isNaN(fecha.getTime())
    ? iso
    : fecha.toLocaleString("es-PE", { dateStyle: "short", timeStyle: "short" });
}

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
  // La baja es seria: pide una confirmación antes de ejecutarse.
  const [confirmandoBaja, setConfirmandoBaja] = useState(false);
  // "Alquilar" abre el alquiler en mostrador (cliente, periodo y contrato), no solo cambia el estado.
  const [alquilando, setAlquilando] = useState(false);
  // "Registrar devolución" abre la devolución del alquiler en curso de este vehículo (con su revisión).
  const [devolviendo, setDevolviendo] = useState<Reserva | null>(null);
  const [buscandoAlquiler, setBuscandoAlquiler] = useState(false);
  // No se halló un alquiler en curso: el vehículo figura alquilado pero sin reserva (por ejemplo, marcado a mano).
  const [sinAlquiler, setSinAlquiler] = useState(false);
  const [error, setError] = useState<string | null>(null);
  // Motivo opcional del cambio de estado (una opción de la lista o "Otro" escrito a mano); se guarda en el historial.
  const [motivoOpcion, setMotivoOpcion] = useState("");
  const [motivoOtro, setMotivoOtro] = useState("");
  const motivo = motivoOpcion === MOTIVO_OTRO ? motivoOtro : motivoOpcion;
  // Historial: null = todavía no se pidió; se carga al pulsar "Ver historial".
  const [historial, setHistorial] = useState<CambioHistorial[] | null>(null);
  const [historialAbierto, setHistorialAbierto] = useState(false);
  const [cargandoHistorial, setCargandoHistorial] = useState(false);

  async function cargarHistorial() {
    setCargandoHistorial(true);
    try {
      setHistorial(await consultarHistorial(sesion, vehiculo.placa));
    } catch (cause) {
      if (cause instanceof SesionExpiradaError) {
        onSesionExpirada();
        return;
      }
      setError(cause instanceof Error ? cause.message : "No se pudo cargar el historial.");
    } finally {
      setCargandoHistorial(false);
    }
  }

  async function alternarHistorial() {
    const abrir = !historialAbierto;
    setHistorialAbierto(abrir);
    if (abrir && historial === null) {
      await cargarHistorial();
    }
  }

  // Tras registrar el alquiler el vehiculo ya figura ALQUILADO: se vuelve a leer para refrescar la tarjeta.
  async function refrescarTrasAlquiler() {
    try {
      onActualizado(await consultarVehiculo(sesion, vehiculo.placa));
      if (historialAbierto) await cargarHistorial();
    } catch (cause) {
      if (cause instanceof SesionExpiradaError) onSesionExpirada();
    }
  }

  // La devolución es parte del alquiler: se busca la reserva en curso de este vehículo para registrarla con su revisión.
  async function buscarAlquilerEnCurso() {
    setError(null);
    setSinAlquiler(false);
    setBuscandoAlquiler(true);
    try {
      const pagina = await listarReservas(sesion, { estado: "EN_CURSO", placa: vehiculo.placa, tamano: 5 });
      if (pagina.reservas.length > 0) {
        setDevolviendo(pagina.reservas[0]);
      } else {
        setSinAlquiler(true);
      }
    } catch (cause) {
      if (cause instanceof SesionExpiradaError || (cause instanceof Error && cause.name === "SesionExpiradaError")) {
        onSesionExpirada();
        return;
      }
      setError(cause instanceof Error ? cause.message : "No se pudo buscar el alquiler de este vehículo.");
    } finally {
      setBuscandoAlquiler(false);
    }
  }

  async function cambiarEstado(destino: EstadoVehiculo) {
    setError(null);
    setConfirmandoBaja(false);
    setPendiente(destino);
    try {
      const actualizado = await actualizarEstado(sesion, vehiculo.placa, destino, motivo);
      setMotivoOpcion("");
      setMotivoOtro("");
      onActualizado(actualizado);
      // Si el historial ya estaba abierto, se refresca para mostrar el cambio nuevo.
      if (historialAbierto) {
        await cargarHistorial();
      }
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
  // El personal solo modifica vehículos de su sucursal (el administrador general, todos).
  const puedeActuar = puedeEditar && puedeCambiarVehiculo(sesion, vehiculo.sucursal);
  // Dar de baja y reactivar un vehículo dado de baja es solo del administrador general.
  const esAdminGeneral = sesion.rol === "ADMINISTRADOR";
  const acciones = ACCIONES[vehiculo.estado].filter(
    (a) => esAdminGeneral || (a.destino !== "FUERA_DE_SERVICIO" && vehiculo.estado !== "FUERA_DE_SERVICIO"),
  );

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

        {puedeEditar && !puedeActuar && (
          <p className="rounded-xl bg-zinc-50 px-3 py-2 text-xs leading-5 text-zinc-500">
            Es de otra sucursal: puedes consultarlo, pero solo su sucursal puede cambiarlo.
          </p>
        )}

        {puedeActuar && acciones.length === 0 && (
          <p className="rounded-xl bg-zinc-50 px-3 py-2 text-xs leading-5 text-zinc-500">
            Está fuera de servicio: solo el administrador general puede reactivarlo.
          </p>
        )}

        {puedeActuar && acciones.length > 0 && (
          <div className="space-y-3 border-t border-zinc-100 pt-4">
            <select
              value={motivoOpcion}
              onChange={(e) => setMotivoOpcion(e.target.value)}
              disabled={pendiente !== null}
              aria-label="Motivo del cambio de estado"
              className="h-10 w-full rounded-xl border border-zinc-200 bg-zinc-50/60 px-3 text-sm text-zinc-900 outline-none focus-visible:border-amber-400 focus-visible:ring-2 focus-visible:ring-amber-300/40 disabled:opacity-50"
            >
              <option value="">Motivo del cambio (opcional)</option>
              {MOTIVOS_CAMBIO[vehiculo.estado].map((m) => (
                <option key={m} value={m}>
                  {m}
                </option>
              ))}
              <option value={MOTIVO_OTRO}>Otro (especificar)...</option>
            </select>
            {motivoOpcion === MOTIVO_OTRO && (
              <Input
                value={motivoOtro}
                onChange={(e) => setMotivoOtro(e.target.value)}
                placeholder="Escribe el motivo"
                maxLength={200}
                disabled={pendiente !== null}
                aria-label="Otro motivo del cambio de estado"
                className="h-10 rounded-xl border-zinc-200 bg-zinc-50/60 px-3 text-sm focus-visible:border-amber-400 focus-visible:ring-amber-300/40"
              />
            )}
          <div className="flex flex-wrap gap-2">
            {acciones.map((accion) => (
              <Button
                key={accion.destino}
                type="button"
                disabled={pendiente !== null || buscandoAlquiler}
                onClick={() => {
                  if (accion.destino === "FUERA_DE_SERVICIO") setConfirmandoBaja(true);
                  else if (vehiculo.estado === "DISPONIBLE" && accion.destino === "ALQUILADO") setAlquilando(true);
                  else if (vehiculo.estado === "ALQUILADO" && accion.destino === "DISPONIBLE") void buscarAlquilerEnCurso();
                  else void cambiarEstado(accion.destino);
                }}
                variant={accion.principal ? "default" : "outline"}
                className={
                  accion.principal
                    ? "h-10 rounded-xl bg-[#ffd400] px-4 font-bold text-zinc-950 hover:bg-[#f2c900]"
                    : "h-10 rounded-xl border-zinc-200 px-4"
                }
              >
                {(pendiente === accion.destino ||
                  (buscandoAlquiler && vehiculo.estado === "ALQUILADO" && accion.destino === "DISPONIBLE")) && (
                  <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
                )}
                {accion.etiqueta}
              </Button>
            ))}
            {sinAlquiler && (
              <div className="w-full space-y-3 rounded-xl bg-amber-50 p-4 ring-1 ring-amber-200">
                <p className="text-sm font-medium text-amber-900">
                  No se encontró un alquiler en curso para este vehículo (quizá se marcó como alquilado a mano). Si
                  lo marcas disponible igualmente, no se cierra ningún contrato ni se registra revisión.
                </p>
                <div className="flex gap-2">
                  <Button
                    type="button"
                    disabled={pendiente !== null}
                    onClick={() => {
                      setSinAlquiler(false);
                      void cambiarEstado("DISPONIBLE");
                    }}
                    className="h-9 rounded-xl bg-amber-500 px-4 text-sm font-semibold text-zinc-950 hover:bg-amber-600"
                  >
                    Marcar disponible de todos modos
                  </Button>
                  <Button type="button" variant="outline" onClick={() => setSinAlquiler(false)} className="h-9 rounded-xl px-4 text-sm">
                    Cancelar
                  </Button>
                </div>
              </div>
            )}
            {confirmandoBaja && (
              <div className="w-full space-y-3 rounded-xl bg-red-50/70 p-4 ring-1 ring-red-200">
                <p className="text-sm font-medium text-red-800">
                  ¿Dar de baja este vehículo? Dejará de aparecer como disponible. No se borra: conserva su historial y
                  puedes reactivarlo después. No se puede si está alquilado o tiene reservas pendientes.
                </p>
                <div className="flex gap-2">
                  <Button
                    type="button"
                    disabled={pendiente !== null}
                    onClick={() => cambiarEstado("FUERA_DE_SERVICIO")}
                    className="h-9 rounded-xl bg-red-600 px-4 text-sm font-semibold text-white hover:bg-red-700"
                  >
                    {pendiente === "FUERA_DE_SERVICIO" && (
                      <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
                    )}
                    Sí, dar de baja
                  </Button>
                  <Button type="button" variant="outline" onClick={() => setConfirmandoBaja(false)} className="h-9 rounded-xl px-4 text-sm">
                    No, volver
                  </Button>
                </div>
              </div>
            )}
            <Button
              type="button"
              variant="ghost"
              onClick={alternarHistorial}
              aria-expanded={historialAbierto}
              className="h-10 rounded-xl px-3 text-zinc-600"
            >
              <History className="size-4" aria-hidden="true" />
              {historialAbierto ? "Ocultar historial" : "Ver historial"}
            </Button>
          </div>
          </div>
        )}

        {alquilando && (
          <AlquilerMostradorModal
            vehiculo={vehiculo}
            sesion={sesion}
            onCerrar={() => setAlquilando(false)}
            onAlquilado={() => void refrescarTrasAlquiler()}
            onSesionExpirada={onSesionExpirada}
          />
        )}

        {devolviendo && (
          <DevolucionModal
            reserva={devolviendo}
            sesion={sesion}
            onCerrar={() => setDevolviendo(null)}
            onCompletada={() => void refrescarTrasAlquiler()}
            onSesionExpirada={onSesionExpirada}
          />
        )}

        {puedeEditar && historialAbierto && (
          <div className="rounded-xl bg-zinc-50 p-3 text-sm" aria-live="polite">
            {cargandoHistorial && (
              <p className="inline-flex items-center gap-2 text-zinc-500">
                <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
                Cargando historial...
              </p>
            )}
            {!cargandoHistorial && historial?.length === 0 && (
              <p className="text-zinc-500">Todavía no hay cambios registrados.</p>
            )}
            {!cargandoHistorial && historial && historial.length > 0 && (
              <ul className="space-y-3">
                {historial.map((cambio, i) => (
                  <li key={`${cambio.fecha}-${i}`} className="border-l-2 border-[#ffd400] pl-3">
                    <p className="font-medium text-zinc-800">
                      {cambio.estadoAnterior === null
                        ? `Alta en la flota (${ESTADOS[cambio.estadoNuevo].texto})`
                        : `${ESTADOS[cambio.estadoAnterior].texto} → ${ESTADOS[cambio.estadoNuevo].texto}`}
                    </p>
                    <p className="text-xs text-zinc-500">
                      {formatearFecha(cambio.fecha)} · {cambio.usuario} ({cambio.rol})
                    </p>
                    {cambio.motivo && (
                      <p className="mt-0.5 text-xs text-zinc-600">Motivo: {cambio.motivo}</p>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
