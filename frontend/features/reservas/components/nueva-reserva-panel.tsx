"use client";

import { useEffect, useState, type FormEvent } from "react";
import { CalendarCheck, CarFront, Check, LoaderCircle, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { buscarCliente } from "@/features/clientes/lib/cliente-api";
import { TIPOS_DOCUMENTO, type Cliente, type TipoDocumento } from "@/features/clientes/types/cliente";
import { CATEGORIAS, SUCURSALES, etiquetaCategoria, etiquetaSucursal } from "@/features/flota/lib/constants";
import {
  consultarDisponibilidad,
  SesionExpiradaError as FlotaSesionExpiradaError,
  type Vehiculo,
} from "@/features/flota/lib/flota-api";
import type { Sesion } from "@/features/flota/lib/session";
import { formatearFecha, hoyEnLima } from "@/features/reservas/lib/constants";
import {
  crearReserva,
  SesionExpiradaError,
  type Reserva,
} from "@/features/reservas/lib/reservas-api";

const CAMPO =
  "h-12 w-full rounded-xl border border-zinc-200 bg-zinc-50/60 px-4 text-sm text-zinc-900 outline-none transition-colors focus-visible:border-amber-400 focus-visible:ring-3 focus-visible:ring-amber-300/40 disabled:opacity-50";

// Aspecto de una opción de vehículo (seleccionada o no).
function opcion(seleccionada: boolean): string {
  return `flex w-full flex-col items-start gap-0.5 rounded-2xl px-4 py-3 text-left text-sm ring-1 transition-colors ${
    seleccionada ? "bg-[#fff9d6] ring-2 ring-amber-400" : "bg-white ring-zinc-200 hover:bg-zinc-50"
  }`;
}

/** Datos con los que se abre el formulario (por ejemplo, desde el catálogo del cliente). */
export interface PrefillReserva {
  categoria?: string;
  sucursal?: string;
  desde?: string;
  hasta?: string;
  /** Vehículo concreto que el cliente eligió en el catálogo (placa y texto para mostrarlo). */
  placa?: string;
  vehiculo?: string;
}

interface NuevaReservaPanelProps {
  sesion: Sesion;
  prefill?: PrefillReserva;
  /** Personal (AGENTE / ADMINISTRADOR / ADMINISTRADOR_SUCURSAL): reserva a nombre de un cliente que busca por documento. */
  esPersonal: boolean;
  onCreada: (reserva: Reserva) => void;
  onSesionExpirada: () => void;
}

/**
 * Formulario de nueva reserva. El cliente reserva para sí mismo; el personal primero busca al
 * cliente por su documento (precondición del documento: el cliente debe existir) y luego reserva.
 * La disponibilidad la verifica el backend con ServicioFlota; si no hay cupo, avisa.
 */
export function NuevaReservaPanel({ sesion, prefill, esPersonal, onCreada, onSesionExpirada }: NuevaReservaPanelProps) {
  const hoy = hoyEnLima();
  // El personal de sucursal solo crea reservas que se recogen en su sede (el administrador general, en cualquiera).
  const recojoFijo = esPersonal && sesion.rol !== "ADMINISTRADOR" && sesion.sucursal ? sesion.sucursal : null;

  const [tipoDocumento, setTipoDocumento] = useState<TipoDocumento>("DNI");
  const [numeroDocumento, setNumeroDocumento] = useState("");
  const [cliente, setCliente] = useState<Cliente | null>(null);
  const [buscandoCliente, setBuscandoCliente] = useState(false);

  const [categoria, setCategoria] = useState(prefill?.categoria ?? "ECONOMICO");
  // Si el cliente eligió un vehículo en el catálogo, se reserva ESE vehículo (si sigue libre).
  const [placaElegida, setPlacaElegida] = useState<string | null>(prefill?.placa ?? null);
  const [recojo, setRecojo] = useState(recojoFijo ?? prefill?.sucursal ?? SUCURSALES[0].value);
  const [devolucion, setDevolucion] = useState(prefill?.sucursal ?? recojoFijo ?? SUCURSALES[0].value);
  // Las fechas empiezan vacías: "Hasta" se habilita recién cuando se elige "Desde".
  const [fechaInicio, setFechaInicio] = useState(prefill?.desde ?? "");
  const [fechaFin, setFechaFin] = useState(prefill?.hasta ?? "");

  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [creada, setCreada] = useState<Reserva | null>(null);

  // Vehículos libres para la categoría, la sucursal de recojo y las fechas elegidas (se consultan a ServicioFlota).
  // "refresco" sube al crear una reserva para volver a consultar (el vehículo reservado ya no está libre).
  const [refresco, setRefresco] = useState(0);
  const [consulta, setConsulta] = useState<{ clave: string; lista?: Vehiculo[]; error?: string } | null>(null);
  const claveActual =
    fechaInicio && fechaFin && fechaFin >= fechaInicio
      ? `${categoria}|${recojo}|${fechaInicio}|${fechaFin}|${refresco}`
      : null;

  useEffect(() => {
    if (!claveActual) return;
    let activo = true;
    // Pequeña espera: si se cambian varios campos seguidos, se consulta una sola vez.
    const espera = setTimeout(() => {
      consultarDisponibilidad(sesion, { sucursal: recojo, categoria, fechaInicio, fechaFin })
        .then((datos) => {
          if (activo) setConsulta({ clave: claveActual, lista: datos.vehiculos });
        })
        .catch((cause: unknown) => {
          if (!activo) return;
          if (cause instanceof FlotaSesionExpiradaError) {
            onSesionExpirada();
            return;
          }
          setConsulta({
            clave: claveActual,
            error: cause instanceof Error ? cause.message : "No se pudo consultar la disponibilidad.",
          });
        });
    }, 300);
    return () => {
      activo = false;
      clearTimeout(espera);
    };
  }, [claveActual, sesion, recojo, categoria, fechaInicio, fechaFin, onSesionExpirada]);

  const resultadoActual = claveActual && consulta?.clave === claveActual ? consulta : null;
  const cargandoDisponibles = claveActual !== null && resultadoActual === null;
  const libres = resultadoActual?.lista ?? null;
  const sinVehiculos = libres !== null && libres.length === 0;
  const elegidoNoLibre = placaElegida !== null && libres !== null && !libres.some((v) => v.placa === placaElegida);

  function manejarError(cause: unknown, porDefecto: string) {
    if (cause instanceof SesionExpiradaError) {
      onSesionExpirada();
      return;
    }
    setError(cause instanceof Error ? cause.message : porDefecto);
  }

  async function buscarPorDocumento() {
    setBuscandoCliente(true);
    setError(null);
    setCliente(null);
    try {
      setCliente(await buscarCliente(sesion.token, tipoDocumento, numeroDocumento));
    } catch (cause) {
      manejarError(cause, "No se pudo buscar al cliente.");
    } finally {
      setBuscandoCliente(false);
    }
  }

  async function enviar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (esPersonal && !cliente) {
      setError("Primero busca al cliente por su documento.");
      return;
    }
    if (!fechaInicio || !fechaFin) {
      setError("Elige las fechas de inicio y de fin.");
      return;
    }
    if (fechaFin < fechaInicio) {
      setError("La fecha de fin no puede ser anterior a la de inicio.");
      return;
    }
    if (elegidoNoLibre) {
      setError("El vehículo que elegiste no está libre en esas fechas. Elige otro de la lista.");
      return;
    }
    setEnviando(true);
    setError(null);
    setCreada(null);
    try {
      const reserva = await crearReserva(sesion, {
        ...(esPersonal && cliente ? { idCliente: cliente.idCliente } : {}),
        categoria,
        ...(placaElegida ? { placa: placaElegida } : {}),
        sucursalRecojo: recojo,
        sucursalDevolucion: devolucion,
        fechaInicio,
        fechaFin,
      });
      setCreada(reserva);
      setPlacaElegida(null);
      setRefresco((n) => n + 1);   // vuelve a consultar: el vehículo reservado ya no está libre
      onCreada(reserva);
    } catch (cause) {
      manejarError(cause, "No se pudo crear la reserva.");
    } finally {
      setEnviando(false);
    }
  }

  return (
    <section aria-labelledby="titulo-nueva" className="max-w-3xl">
      <Card className="rounded-3xl border-0 bg-white shadow-[0_18px_60px_-32px_rgba(24,24,27,0.28)] ring-1 ring-zinc-200/80">
        <CardContent className="p-5 sm:p-7">
          <h2 id="titulo-nueva" className="text-lg font-semibold tracking-tight text-zinc-950">
            Nueva reserva
          </h2>
          <p className="mt-1 text-sm text-zinc-500">
            {esPersonal
              ? "Busca al cliente por su documento y elige la categoría, la sucursal y las fechas."
              : "Elige la categoría, la sucursal y las fechas de tu alquiler."}
          </p>

          <form onSubmit={enviar} className="mt-6 space-y-5">
            {esPersonal && (
              <fieldset className="space-y-3 rounded-2xl bg-zinc-50/70 p-4 ring-1 ring-zinc-200/70">
                <legend className="px-1 text-sm font-semibold text-zinc-800">Cliente</legend>
                <div className="grid gap-3 sm:grid-cols-[1fr_2fr_auto] sm:items-end">
                  <div className="space-y-2">
                    <label htmlFor="res-tipo-doc" className="text-sm font-medium text-zinc-700">
                      Documento
                    </label>
                    <select
                      id="res-tipo-doc"
                      value={tipoDocumento}
                      onChange={(e) => {
                        setTipoDocumento(e.target.value as TipoDocumento);
                        setCliente(null);
                      }}
                      className={CAMPO}
                    >
                      {TIPOS_DOCUMENTO.map((t) => (
                        <option key={t} value={t}>
                          {t}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="space-y-2">
                    <label htmlFor="res-num-doc" className="text-sm font-medium text-zinc-700">
                      Número
                    </label>
                    <Input
                      id="res-num-doc"
                      value={numeroDocumento}
                      onChange={(e) => {
                        setNumeroDocumento(e.target.value);
                        setCliente(null);
                      }}
                      maxLength={20}
                      autoComplete="off"
                      className="h-12 rounded-xl border-zinc-200 bg-white px-4 text-sm"
                    />
                  </div>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={buscarPorDocumento}
                    disabled={buscandoCliente || numeroDocumento.trim() === ""}
                    className="h-12 rounded-xl border-zinc-300 px-5"
                  >
                    {buscandoCliente ? (
                      <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
                    ) : (
                      <Search className="size-4" aria-hidden="true" />
                    )}
                    Buscar
                  </Button>
                </div>
                {cliente && (
                  <p className="rounded-xl bg-emerald-50 px-3 py-2 text-sm text-emerald-800 ring-1 ring-emerald-200">
                    Cliente: <strong>{cliente.nombres} {cliente.apellidos}</strong> · {cliente.email}
                  </p>
                )}
              </fieldset>
            )}

            {placaElegida && libres === null && (
              <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-[#fff9d6] px-4 py-3 ring-1 ring-amber-200">
                <p className="text-sm text-zinc-800">
                  <span className="font-semibold">Vehículo elegido:</span>{" "}
                  {prefill?.vehiculo ? `${prefill.vehiculo} · ` : ""}placa {placaElegida}
                  <span className="block text-xs text-zinc-500">
                    Se reserva este vehículo si sigue libre en esas fechas.
                  </span>
                </p>
                <Button type="button" variant="outline" onClick={() => setPlacaElegida(null)} className="h-9 rounded-xl px-3 text-sm">
                  Quitar elección
                </Button>
              </div>
            )}

            <div className="grid gap-4 sm:grid-cols-3">
              <div className="space-y-2 sm:col-span-1">
                <label htmlFor="res-categoria" className="text-sm font-semibold text-zinc-800">
                  Categoría
                </label>
                <select
                  id="res-categoria"
                  value={categoria}
                  onChange={(e) => {
                    setCategoria(e.target.value);
                    setPlacaElegida(null); // otra categoría: el vehículo elegido ya no corresponde
                  }}
                  className={CAMPO}
                >
                  {CATEGORIAS.map((c) => (
                    <option key={c.value} value={c.value}>
                      {c.label}
                    </option>
                  ))}
                </select>
              </div>
              <div className="space-y-2">
                <label htmlFor="res-recojo" className="text-sm font-semibold text-zinc-800">
                  Sucursal de recojo
                </label>
                <select
                  id="res-recojo"
                  value={recojo}
                  onChange={(e) => {
                    setRecojo(e.target.value);
                    setPlacaElegida(null); // otra sucursal: el vehículo elegido ya no corresponde
                  }}
                  disabled={recojoFijo !== null}
                  className={CAMPO}
                >
                  {SUCURSALES.map((s) => (
                    <option key={s.value} value={s.value}>
                      {s.label}
                    </option>
                  ))}
                </select>
                {recojoFijo !== null && (
                  <p className="text-xs text-zinc-400">Solo puedes crear reservas que se recojan en tu sucursal.</p>
                )}
              </div>
              <div className="space-y-2">
                <label htmlFor="res-devolucion" className="text-sm font-semibold text-zinc-800">
                  Sucursal de devolución
                </label>
                <select id="res-devolucion" value={devolucion} onChange={(e) => setDevolucion(e.target.value)} className={CAMPO}>
                  {SUCURSALES.map((s) => (
                    <option key={s.value} value={s.value}>
                      {s.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <label htmlFor="res-inicio" className="text-sm font-semibold text-zinc-800">
                  Desde
                </label>
                <input
                  id="res-inicio"
                  type="date"
                  min={hoy}
                  value={fechaInicio}
                  onChange={(e) => {
                    setFechaInicio(e.target.value);
                    if (fechaFin && fechaFin < e.target.value) setFechaFin("");
                  }}
                  required
                  className={CAMPO}
                />
              </div>
              <div className="space-y-2">
                <label htmlFor="res-fin" className="text-sm font-semibold text-zinc-800">
                  Hasta
                </label>
                <input
                  id="res-fin"
                  type="date"
                  min={fechaInicio}
                  value={fechaFin}
                  onChange={(e) => setFechaFin(e.target.value)}
                  disabled={!fechaInicio}
                  required
                  className={CAMPO}
                />
                {!fechaInicio && <p className="text-xs text-zinc-400">Elige primero la fecha de inicio.</p>}
              </div>
            </div>

            {claveActual && (
              <div className="space-y-3" aria-live="polite">
                <p className="text-sm font-semibold text-zinc-800">Vehículos disponibles</p>
                {cargandoDisponibles && (
                  <p className="inline-flex items-center gap-2 text-sm text-zinc-500">
                    <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
                    Buscando los vehículos libres en esas fechas...
                  </p>
                )}
                {resultadoActual?.error && (
                  <div role="alert" className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                    {resultadoActual.error}
                  </div>
                )}
                {sinVehiculos && (
                  <p className="rounded-xl bg-zinc-50 px-4 py-3 text-sm text-zinc-600">
                    No hay vehículos {etiquetaCategoria(categoria)} libres en {etiquetaSucursal(recojo)} en esas fechas.
                    Prueba con otras fechas u otra categoría.
                  </p>
                )}
                {libres !== null && libres.length > 0 && (
                  <>
                    <p className="text-xs text-zinc-500">
                      {libres.length} {libres.length === 1 ? "vehículo libre" : "vehículos libres"} en esas fechas. Elige el
                      que quieres o deja que te asignemos uno.
                    </p>
                    <div className="grid gap-3 sm:grid-cols-2">
                      <button
                        type="button"
                        aria-pressed={placaElegida === null}
                        onClick={() => setPlacaElegida(null)}
                        className={opcion(placaElegida === null)}
                      >
                        <span className="flex w-full items-center justify-between font-semibold text-zinc-900">
                          Cualquiera de la categoría
                          {placaElegida === null && <Check className="size-4 text-amber-600" aria-hidden="true" />}
                        </span>
                        <span className="text-xs text-zinc-500">Te asignamos uno de los disponibles</span>
                      </button>
                      {libres.map((v) => (
                        <button
                          key={v.placa}
                          type="button"
                          aria-pressed={placaElegida === v.placa}
                          onClick={() => setPlacaElegida(v.placa)}
                          className={opcion(placaElegida === v.placa)}
                        >
                          <span className="flex w-full items-center justify-between font-semibold text-zinc-900">
                            <span className="inline-flex items-center gap-2">
                              <CarFront className="size-4 text-zinc-400" aria-hidden="true" />
                              {v.marca} {v.modelo}
                            </span>
                            {placaElegida === v.placa && <Check className="size-4 text-amber-600" aria-hidden="true" />}
                          </span>
                          <span className="text-xs text-zinc-500">
                            Año {v.anio} · placa {v.placa}
                          </span>
                        </button>
                      ))}
                    </div>
                  </>
                )}
                {elegidoNoLibre && (
                  <p className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
                    El vehículo que elegiste ({placaElegida}) no está libre en esas fechas. Elige otro de la lista.
                  </p>
                )}
              </div>
            )}

            {error && (
              <div role="alert" className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm leading-5 text-red-700">
                {error}
              </div>
            )}

            {creada && (
              <div role="status" className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm leading-5 text-emerald-800">
                <p className="font-semibold">Reserva confirmada</p>
                <p className="mt-1">
                  {etiquetaCategoria(creada.categoria)} · recojo en {etiquetaSucursal(creada.sucursalRecojo)} ·{" "}
                  {formatearFecha(creada.fechaInicio)} al {formatearFecha(creada.fechaFin)}
                </p>
                <p className="mt-1 text-xs text-emerald-700">Código: {creada.idReserva}</p>
              </div>
            )}

            <Button
              type="submit"
              disabled={enviando || sinVehiculos || elegidoNoLibre}
              className="h-12 rounded-xl bg-[#ffd400] px-7 text-sm font-bold text-zinc-950 shadow-[0_7px_18px_-9px_rgba(234,179,8,0.8)] hover:bg-[#f2c900]"
            >
              {enviando ? (
                <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
              ) : (
                <CalendarCheck className="size-4" aria-hidden="true" />
              )}
              Confirmar reserva
            </Button>
          </form>
        </CardContent>
      </Card>
    </section>
  );
}
