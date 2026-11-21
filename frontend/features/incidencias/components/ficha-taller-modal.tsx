"use client";

import { useEffect, useState, type ReactNode } from "react";
import { CheckCircle2, LoaderCircle, Plus, Save, Trash2, Wrench } from "lucide-react";
import { Button } from "@/components/ui/button";
import { VentanaModal } from "@/components/ventana-modal";
import { VehiculoVistas, tipoCarroceria, type PinVista } from "@/features/contratos/components/vehiculo-diagrama";
import { obtenerContrato } from "@/features/contratos/lib/contratos-api";
import {
  GRAVEDADES,
  TIPOS_DANO,
  etiquetaGravedad,
  etiquetaTipoDano,
  etiquetaVista,
  etiquetaZona,
  formatoSoles,
} from "@/features/contratos/lib/revision";
import type { Contrato, GravedadDano, TipoDano, VistaVehiculo } from "@/features/contratos/types/contrato";
import { etiquetaCategoria } from "@/features/flota/lib/constants";
import { consultarVehiculo, type Vehiculo } from "@/features/flota/lib/flota-api";
import type { Sesion } from "@/features/flota/lib/session";
import {
  SISTEMAS,
  errorFicha,
  externoNuevo,
  fichaAPayload,
  formDesdeFicha,
  internoNuevo,
  totalDanos,
  type ExternoForm,
  type FichaForm,
  type InternoForm,
} from "@/features/incidencias/lib/ficha-taller";
import {
  agregarSeguimiento,
  cerrarIncidencia,
  guardarFicha,
  mensajeErrorIncidencia,
  obtenerFicha,
} from "@/features/incidencias/lib/incidencias-api";
import type { FichaTaller, Incidencia, SistemaVehiculo } from "@/features/incidencias/types/incidencia";
import { ApiRequestError } from "@/lib/api-client";

const CAMPO =
  "h-10 w-full rounded-xl border border-zinc-200 bg-white px-3 text-sm text-zinc-900 outline-none transition-colors focus-visible:border-amber-400 focus-visible:ring-3 focus-visible:ring-amber-300/40 disabled:opacity-60 disabled:bg-zinc-50";
const AREA =
  "w-full rounded-xl border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 outline-none transition-colors focus-visible:border-amber-400 focus-visible:ring-3 focus-visible:ring-amber-300/40 disabled:opacity-60 disabled:bg-zinc-50";
const LETRAS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

function fecha(iso: string | null | undefined): string {
  if (!iso) return "—";
  const valor = new Date(`${iso}T12:00:00`);
  return Number.isNaN(valor.getTime()) ? iso : new Intl.DateTimeFormat("es-PE", { dateStyle: "medium" }).format(valor);
}

function fechaHora(iso: string): string {
  const valor = new Date(iso);
  return Number.isNaN(valor.getTime()) ? iso : new Intl.DateTimeFormat("es-PE", { dateStyle: "medium", timeStyle: "short" }).format(valor);
}

function Seccion({ titulo, ayuda, children }: { titulo: string; ayuda?: string; children: ReactNode }) {
  return (
    <section className="space-y-3 rounded-2xl bg-zinc-50/70 p-4 ring-1 ring-zinc-200/70">
      <div>
        <h3 className="text-sm font-semibold text-zinc-800">{titulo}</h3>
        {ayuda ? <p className="mt-0.5 text-xs leading-5 text-zinc-500">{ayuda}</p> : null}
      </div>
      {children}
    </section>
  );
}

interface FichaTallerModalProps {
  incidencia: Incidencia;
  sesion: Sesion;
  onCerrar: () => void;
  /** Se llama con la incidencia ya actualizada (ficha guardada o incidencia cerrada) para refrescar su tarjeta. */
  onActualizada: (incidencia: Incidencia) => void;
  onSesionExpirada: () => void;
}

/**
 * Ficha técnica del taller: lo que el mecánico diagnostica y registra mientras el vehículo está en mantenimiento
 * (daños externos sobre el dibujo, daños internos, días en taller, costos y avances) y lo que deja al cerrar.
 */
export function FichaTallerModal({ incidencia, sesion, onCerrar, onActualizada, onSesionExpirada }: FichaTallerModalProps) {
  const [ficha, setFicha] = useState<FichaTaller | null>(null);
  const [form, setForm] = useState<FichaForm>(formDesdeFicha(null));
  const [vehiculo, setVehiculo] = useState<Vehiculo | null>(null);
  const [contrato, setContrato] = useState<Contrato | null>(null);
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [cerrando, setCerrando] = useState(false);
  const [anotando, setAnotando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);
  const [sinGuardar, setSinGuardar] = useState(false);
  const [confirmaSalida, setConfirmaSalida] = useState(false);
  const [seleccionado, setSeleccionado] = useState<number | null>(null);
  const [nota, setNota] = useState("");
  const [trabajo, setTrabajo] = useState("");
  const [costoFinal, setCostoFinal] = useState("");

  const cerrada = incidencia.estado === "CERRADA" || ficha?.estadoIncidencia === "CERRADA";
  const ocupado = guardando || cerrando;

  useEffect(() => {
    let activo = true;
    Promise.all([
      obtenerFicha(sesion.token, incidencia.idIncidencia),
      consultarVehiculo(sesion, incidencia.idVehiculo).catch(() => null),
      incidencia.idContrato ? obtenerContrato(sesion.token, incidencia.idContrato).catch(() => null) : Promise.resolve(null),
    ])
      .then(([f, v, c]) => {
        if (!activo) return;
        setFicha(f);
        setForm(formDesdeFicha(f));
        setVehiculo(v);
        setContrato(c);
        setCargando(false);
      })
      .catch((cause: unknown) => {
        if (!activo) return;
        if (cause instanceof ApiRequestError && cause.status === 401) {
          onSesionExpirada();
          return;
        }
        setError(mensajeErrorIncidencia(cause));
        setCargando(false);
      });
    return () => {
      activo = false;
    };
  }, [sesion, incidencia.idIncidencia, incidencia.idVehiculo, incidencia.idContrato, onSesionExpirada]);

  function editar(nuevo: FichaForm) {
    setForm(nuevo);
    setSinGuardar(true);
    setConfirmaSalida(false);
    setAviso(null);
  }

  function manejarError(cause: unknown) {
    if (cause instanceof ApiRequestError && cause.status === 401) {
      onSesionExpirada();
      return;
    }
    setError(mensajeErrorIncidencia(cause));
  }

  // ---------------------------------------------------------------- dibujo
  const devolucion = contrato?.devolucion ?? null;
  const entrega = contrato?.entrega ?? null;
  const danosCliente = (devolucion?.danos ?? []).filter((d) => d.vista && d.posX != null && d.posY != null);
  const pines: PinVista[] = [
    ...(entrega?.danos ?? []).flatMap((d, i) =>
      d.vista && d.posX != null && d.posY != null
        ? [{ id: `previo-${i}`, vista: d.vista, x: d.posX, y: d.posY, numero: LETRAS[i % 26], tono: "previo" as const, titulo: `Ya estaba al entregar: ${d.descripcion}` }]
        : [],
    ),
    ...(devolucion?.danos ?? []).flatMap((d, i) =>
      d.vista && d.posX != null && d.posY != null
        ? [{ id: `cliente-${i}`, vista: d.vista, x: d.posX, y: d.posY, numero: i + 1, tono: "nuevo" as const, titulo: `Reportado en la devolución: ${d.descripcion}` }]
        : [],
    ),
    ...form.externos.map((d, i) => ({
      id: d.clave,
      vista: d.vista,
      x: d.x,
      y: d.y,
      numero: (devolucion?.danos.length ?? 0) + i + 1,
      tono: "taller" as const,
      seleccionado: d.clave === seleccionado,
      titulo: `Agregado por el taller: ${d.descripcion || etiquetaTipoDano(d.tipo)}`,
    })),
  ];
  const tipo = tipoCarroceria(vehiculo?.categoria);

  function marcarEnDibujo(vista: VistaVehiculo, x: number, y: number) {
    if (cerrada || ocupado) return;
    const nuevo = externoNuevo(vista, x, y);
    editar({ ...form, externos: [...form.externos, nuevo] });
    setSeleccionado(nuevo.clave);
  }

  function cambiarExterno(clave: number, cambios: Partial<ExternoForm>) {
    editar({ ...form, externos: form.externos.map((d) => (d.clave === clave ? { ...d, ...cambios } : d)) });
  }

  function cambiarInterno(clave: number, cambios: Partial<InternoForm>) {
    editar({ ...form, internos: form.internos.map((d) => (d.clave === clave ? { ...d, ...cambios } : d)) });
  }

  // ---------------------------------------------------------------- acciones
  async function guardar(): Promise<boolean> {
    const problema = errorFicha(form);
    if (problema) {
      setError(problema);
      return false;
    }
    setGuardando(true);
    setError(null);
    try {
      const guardada = await guardarFicha(sesion.token, incidencia.idIncidencia, fichaAPayload(form));
      setFicha(guardada);
      setForm(formDesdeFicha(guardada));
      setSinGuardar(false);
      setAviso("Ficha guardada.");
      onActualizada({ ...incidencia, tieneFicha: true, fechaSalidaPrevista: guardada.fechaSalidaPrevista });
      return true;
    } catch (cause) {
      manejarError(cause);
      return false;
    } finally {
      setGuardando(false);
    }
  }

  async function anotar() {
    if (!nota.trim()) return;
    setAnotando(true);
    setError(null);
    try {
      const actualizada = await agregarSeguimiento(sesion.token, incidencia.idIncidencia, nota.trim());
      setFicha((actual) => (actual ? { ...actual, seguimiento: actualizada.seguimiento } : actualizada));
      setNota("");
    } catch (cause) {
      manejarError(cause);
    } finally {
      setAnotando(false);
    }
  }

  async function cerrarIncidenciaYLiberar() {
    const costo = Number(costoFinal);
    if (!trabajo.trim()) {
      setError("Indica qué trabajo se realizó para poder cerrar la incidencia.");
      return;
    }
    if (costoFinal.trim() === "" || !Number.isFinite(costo) || costo < 0) {
      setError("Indica el costo final de la reparación (un número mayor o igual a 0).");
      return;
    }
    setCerrando(true);
    setError(null);
    try {
      if (sinGuardar && !(await guardar())) return;
      const cerradaOk = await cerrarIncidencia(sesion.token, incidencia.idIncidencia, {
        trabajoRealizado: trabajo.trim(),
        costoFinal: Math.round(costo * 100) / 100,
      });
      onActualizada(cerradaOk);
      setFicha(await obtenerFicha(sesion.token, incidencia.idIncidencia));
      setAviso("Incidencia cerrada. Si era la última abierta de este vehículo, ya vuelve a estar disponible.");
    } catch (cause) {
      manejarError(cause);
    } finally {
      setCerrando(false);
    }
  }

  function intentarCerrar() {
    if (sinGuardar && !cerrada && !confirmaSalida) {
      setConfirmaSalida(true);
      setError("Tienes cambios sin guardar. Pulsa «Guardar ficha», o «Cerrar ventana» otra vez para descartarlos.");
      return;
    }
    onCerrar();
  }

  const repuestos = Number(form.repuestos) || 0;
  const manoObra = Number(form.manoObra) || 0;
  const planTotal = repuestos + manoObra;
  const diasNumero = form.dias.trim() === "" ? null : Number(form.dias);

  return (
    <VentanaModal
      titulo="Ficha técnica del taller"
      subtitulo={
        <>
          <span className="font-mono">{incidencia.idVehiculo}</span>
          {vehiculo ? ` · ${vehiculo.marca} ${vehiculo.modelo} · ${etiquetaCategoria(vehiculo.categoria)}` : ""}
        </>
      }
      onCerrar={intentarCerrar}
      ocupado={ocupado}
      ancho="max-w-4xl"
    >
      {cargando ? (
        <p className="mt-8 flex items-center justify-center gap-2 pb-4 text-sm text-zinc-500">
          <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
          Cargando la ficha...
        </p>
      ) : (
        <div className="mt-5 space-y-5">
          {/* ------------------------------------------------------------ resumen */}
          <div className="grid gap-3 rounded-2xl bg-white p-4 text-sm ring-1 ring-zinc-200 sm:grid-cols-3">
            <div className="sm:col-span-3">
              <p className="text-xs text-zinc-400">Incidencia {incidencia.idIncidencia.slice(0, 8).toUpperCase()}</p>
              <p className="mt-0.5 whitespace-pre-wrap text-zinc-700">{incidencia.descripcion}</p>
            </div>
            <div>
              <p className="text-xs text-zinc-400">Ingresó a taller</p>
              <p className="font-medium text-zinc-800">{fecha(ficha?.ingresoATaller)}</p>
            </div>
            <div>
              <p className="text-xs text-zinc-400">Sale (previsto)</p>
              <p className="font-medium text-zinc-800">
                {ficha?.fechaSalidaPrevista ? fecha(ficha.fechaSalidaPrevista) : "Sin estimar"}
              </p>
            </div>
            <div>
              <p className="text-xs text-zinc-400">Estado</p>
              <p className={`font-semibold ${cerrada ? "text-emerald-700" : "text-red-700"}`}>{cerrada ? "Cerrada" : "En taller"}</p>
            </div>
          </div>

          <fieldset disabled={cerrada || ocupado} className="space-y-5">
            {/* ------------------------------------------------------------ carrocería */}
            <Seccion
              titulo="Daños de carrocería"
              ayuda={
                cerrada
                  ? "Los daños que quedaron registrados."
                  : "Haz clic en el dibujo para marcar un daño más que encuentres. Las marcas rojas son las que se reportaron en la devolución."
              }
            >
              <VehiculoVistas tipo={tipo} pines={pines} onColocar={cerrada ? undefined : marcarEnDibujo} onSeleccionar={(id) => typeof id === "number" && setSeleccionado(id)} columnas={3} />
              <p className="text-[11px] text-zinc-500">
                <span className="font-semibold text-zinc-600">Gris:</span> ya estaba al entregar ·{" "}
                <span className="font-semibold text-red-600">Rojo:</span> reportado en la devolución (del cliente) ·{" "}
                <span className="font-semibold text-orange-600">Naranja:</span> agregado por el taller
              </p>

              {danosCliente.length > 0 && (
                <div className="rounded-xl bg-white p-3 ring-1 ring-zinc-200">
                  <p className="text-xs font-semibold text-zinc-700">Reportados en la devolución (a cargo del cliente)</p>
                  <ul className="mt-1 space-y-0.5 text-xs text-zinc-600">
                    {(devolucion?.danos ?? []).map((d, i) => (
                      <li key={i}>
                        <strong>{i + 1})</strong> {d.vista ? etiquetaVista(d.vista) : etiquetaZona(d.zona)}
                        {d.tipo ? ` · ${etiquetaTipoDano(d.tipo)}` : ""} ({etiquetaGravedad(d.gravedad).toLowerCase()}): {d.descripcion}
                        {d.costoEstimado != null ? <strong> — {formatoSoles(d.costoEstimado)}</strong> : null}
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {form.externos.length > 0 && (
                <ul className="space-y-2">
                  {form.externos.map((d, i) => (
                    <li
                      key={d.clave}
                      onClick={() => setSeleccionado(d.clave)}
                      className={`space-y-2 rounded-xl bg-white p-3 ring-1 ${d.clave === seleccionado ? "ring-2 ring-amber-400" : "ring-zinc-200"}`}
                    >
                      <div className="flex items-center justify-between">
                        <span className="flex items-center gap-2 text-xs font-semibold text-zinc-600">
                          <span className="grid size-6 place-items-center rounded-full bg-orange-600 text-[11px] font-bold text-white">
                            {(devolucion?.danos.length ?? 0) + i + 1}
                          </span>
                          {etiquetaVista(d.vista)} · agregado por el taller
                        </span>
                        <button
                          type="button"
                          onClick={() => editar({ ...form, externos: form.externos.filter((e) => e.clave !== d.clave) })}
                          aria-label={`Quitar el daño ${(devolucion?.danos.length ?? 0) + i + 1}`}
                          className="rounded-md p-1 text-zinc-400 hover:bg-red-50 hover:text-red-600"
                        >
                          <Trash2 className="size-4" aria-hidden="true" />
                        </button>
                      </div>
                      <div className="grid gap-2 sm:grid-cols-3">
                        <select value={d.tipo} onChange={(e) => cambiarExterno(d.clave, { tipo: e.target.value as TipoDano })} aria-label="Tipo de daño" className={CAMPO}>
                          {TIPOS_DANO.map((t) => (
                            <option key={t.value} value={t.value}>{t.label}</option>
                          ))}
                        </select>
                        <select value={d.gravedad} onChange={(e) => cambiarExterno(d.clave, { gravedad: e.target.value as GravedadDano })} aria-label="Gravedad" className={CAMPO}>
                          {GRAVEDADES.map((g) => (
                            <option key={g.value} value={g.value}>{g.label}</option>
                          ))}
                        </select>
                        <input type="number" inputMode="decimal" min={0} step="0.01" value={d.costo} onChange={(e) => cambiarExterno(d.clave, { costo: e.target.value })} placeholder="Costo estimado (S/)" aria-label="Costo estimado" className={CAMPO} />
                      </div>
                      <input value={d.descripcion} onChange={(e) => cambiarExterno(d.clave, { descripcion: e.target.value })} maxLength={300} placeholder="Describe el daño y qué hay que hacer" aria-label="Descripción del daño" className={CAMPO} />
                    </li>
                  ))}
                </ul>
              )}
            </Seccion>

            {/* ------------------------------------------------------------ internos */}
            <Seccion titulo="Daños internos o mecánicos" ayuda="Lo que no se ve por fuera: motor, frenos, transmisión, suspensión, sistema eléctrico...">
              {form.internos.length === 0 && <p className="rounded-xl bg-white px-3 py-2.5 text-sm text-zinc-600 ring-1 ring-zinc-200">Sin daños internos registrados.</p>}
              <ul className="space-y-2">
                {form.internos.map((d, i) => (
                  <li key={d.clave} className="space-y-2 rounded-xl bg-white p-3 ring-1 ring-zinc-200">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-semibold text-zinc-600">Daño interno {i + 1}</span>
                      <button type="button" onClick={() => editar({ ...form, internos: form.internos.filter((e) => e.clave !== d.clave) })} aria-label={`Quitar el daño interno ${i + 1}`} className="rounded-md p-1 text-zinc-400 hover:bg-red-50 hover:text-red-600">
                        <Trash2 className="size-4" aria-hidden="true" />
                      </button>
                    </div>
                    <div className="grid gap-2 sm:grid-cols-3">
                      <select value={d.sistema} onChange={(e) => cambiarInterno(d.clave, { sistema: e.target.value as SistemaVehiculo })} aria-label="Sistema" className={CAMPO}>
                        {SISTEMAS.map((s) => (
                          <option key={s.value} value={s.value}>{s.label}</option>
                        ))}
                      </select>
                      <select value={d.gravedad} onChange={(e) => cambiarInterno(d.clave, { gravedad: e.target.value as GravedadDano })} aria-label="Gravedad" className={CAMPO}>
                        {GRAVEDADES.map((g) => (
                          <option key={g.value} value={g.value}>{g.label}</option>
                        ))}
                      </select>
                      <input type="number" inputMode="decimal" min={0} step="0.01" value={d.costo} onChange={(e) => cambiarInterno(d.clave, { costo: e.target.value })} placeholder="Costo estimado (S/)" aria-label="Costo estimado" className={CAMPO} />
                    </div>
                    <input value={d.descripcion} onChange={(e) => cambiarInterno(d.clave, { descripcion: e.target.value })} maxLength={300} placeholder="Describe el daño (ej. radiador roto, motor sobrecalentado)" aria-label="Descripción del daño interno" className={CAMPO} />
                  </li>
                ))}
              </ul>
              <Button type="button" variant="outline" size="sm" disabled={form.internos.length >= 30} onClick={() => editar({ ...form, internos: [...form.internos, internoNuevo()] })} className="rounded-xl">
                <Plus aria-hidden="true" />
                Agregar daño interno
              </Button>
            </Seccion>

            {/* ------------------------------------------------------------ plan */}
            <Seccion titulo="Diagnóstico y plan de reparación" ayuda="Con los días estimados el sistema calcula cuándo sale el vehículo del taller.">
              <label className="block space-y-1.5 text-xs font-medium text-zinc-600">
                Diagnóstico
                <textarea value={form.diagnostico} onChange={(e) => editar({ ...form, diagnostico: e.target.value })} maxLength={1000} rows={3} placeholder="Qué se encontró y qué hay que hacer" className={AREA} />
              </label>
              <div className="grid gap-3 sm:grid-cols-3">
                <label className="space-y-1.5 text-xs font-medium text-zinc-600">
                  Días estimados en taller
                  <input type="number" inputMode="numeric" min={0} max={365} step={1} value={form.dias} onChange={(e) => editar({ ...form, dias: e.target.value })} placeholder="Ej. 4" className={CAMPO} />
                </label>
                <label className="space-y-1.5 text-xs font-medium text-zinc-600">
                  Repuestos (S/)
                  <input type="number" inputMode="decimal" min={0} step="0.01" value={form.repuestos} onChange={(e) => editar({ ...form, repuestos: e.target.value })} placeholder="0.00" className={CAMPO} />
                </label>
                <label className="space-y-1.5 text-xs font-medium text-zinc-600">
                  Mano de obra (S/)
                  <input type="number" inputMode="decimal" min={0} step="0.01" value={form.manoObra} onChange={(e) => editar({ ...form, manoObra: e.target.value })} placeholder="0.00" className={CAMPO} />
                </label>
              </div>
              <div className="flex flex-wrap items-center justify-between gap-2 rounded-xl bg-white px-3 py-2 text-sm ring-1 ring-zinc-200">
                <span className="text-zinc-600">
                  Plan de reparación: <strong className="text-zinc-900">{formatoSoles(planTotal)}</strong>
                  <span className="text-zinc-400"> · daños anotados: {formatoSoles(totalDanos(form))}</span>
                </span>
                <span className="text-zinc-600">
                  {diasNumero != null && Number.isFinite(diasNumero) && ficha?.ingresoATaller
                    ? `Saldría el ${fecha(new Date(new Date(`${ficha.ingresoATaller}T12:00:00`).getTime() + diasNumero * 86_400_000).toISOString().slice(0, 10))}`
                    : "Indica los días para calcular la salida"}
                </span>
              </div>
            </Seccion>
          </fieldset>

          {/* ------------------------------------------------------------ seguimiento */}
          <Seccion titulo="Seguimiento" ayuda="Una nota por avance, con quién y cuándo. Se guarda en el momento.">
            {!cerrada && (
              <div className="flex flex-wrap gap-2">
                <input value={nota} onChange={(e) => setNota(e.target.value)} onKeyDown={(e) => { if (e.key === "Enter") { e.preventDefault(); void anotar(); } }} maxLength={500} placeholder="Ej. Llegó el radiador nuevo; mañana se instala" aria-label="Nueva nota de avance" className={`${CAMPO} min-w-0 flex-1`} />
                <Button type="button" onClick={() => void anotar()} disabled={anotando || !nota.trim()} className="h-10 rounded-xl bg-zinc-950 px-4 text-white hover:bg-zinc-800">
                  {anotando && <LoaderCircle className="animate-spin" aria-hidden="true" />}
                  Agregar nota
                </Button>
              </div>
            )}
            {(ficha?.seguimiento ?? []).length === 0 ? (
              <p className="text-xs text-zinc-500">Todavía no hay avances registrados.</p>
            ) : (
              <ul className="space-y-2">
                {(ficha?.seguimiento ?? []).map((s, i) => (
                  <li key={i} className="border-l-2 border-[#ffd400] pl-3 text-sm">
                    <p className="text-zinc-800">{s.nota}</p>
                    <p className="text-xs text-zinc-500">{fechaHora(s.registradoEn)} · {s.registradoPor}</p>
                  </li>
                ))}
              </ul>
            )}
          </Seccion>

          {/* ------------------------------------------------------------ cierre */}
          {cerrada ? (
            <Seccion titulo="Cierre">
              <div className="rounded-xl bg-emerald-50 p-3 text-sm text-emerald-900 ring-1 ring-emerald-200">
                <p className="font-semibold">Reparación terminada</p>
                <p className="mt-1">{ficha?.trabajoRealizado ?? "Se cerró sin detallar el trabajo realizado."}</p>
                <p className="mt-1 text-xs">
                  {ficha?.costoFinal != null ? `Costo final: ${formatoSoles(ficha.costoFinal)}` : ""}
                  {ficha?.costoFinal != null && (ficha.costoRepuestos != null || ficha.costoManoObra != null)
                    ? ` · Plan: ${formatoSoles((ficha.costoRepuestos ?? 0) + (ficha.costoManoObra ?? 0))}`
                    : ""}
                  {ficha?.actualizadoPor ? ` · ${ficha.actualizadoPor}` : ""}
                </p>
              </div>
            </Seccion>
          ) : (
            <Seccion titulo="Cerrar la incidencia" ayuda="Al cerrarla, si no quedan otras incidencias abiertas, el vehículo vuelve a estar disponible. Queda constancia de lo que se reparó.">
              <label className="block space-y-1.5 text-xs font-medium text-zinc-600">
                Trabajo realizado
                <textarea value={trabajo} onChange={(e) => setTrabajo(e.target.value)} maxLength={1000} rows={2} placeholder="Qué se reparó o cambió" disabled={ocupado} className={AREA} />
              </label>
              <div className="flex flex-wrap items-end gap-3">
                <label className="space-y-1.5 text-xs font-medium text-zinc-600">
                  Costo final de la reparación (S/)
                  <input type="number" inputMode="decimal" min={0} step="0.01" value={costoFinal} onChange={(e) => setCostoFinal(e.target.value)} placeholder="0.00" disabled={ocupado} className={`${CAMPO} w-44`} />
                </label>
                <Button type="button" onClick={() => void cerrarIncidenciaYLiberar()} disabled={ocupado} className="h-10 rounded-xl bg-emerald-600 px-4 font-bold text-white hover:bg-emerald-700">
                  {cerrando ? <LoaderCircle className="animate-spin" aria-hidden="true" /> : <CheckCircle2 aria-hidden="true" />}
                  Cerrar incidencia y liberar vehículo
                </Button>
              </div>
            </Seccion>
          )}

          {error && (
            <div role="alert" className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm leading-5 text-red-700">
              {error}
            </div>
          )}
          {aviso && !error && <p role="status" className="text-sm font-medium text-emerald-700">{aviso}</p>}

          <div className="flex flex-wrap justify-end gap-2 border-t border-zinc-100 pt-4">
            <Button type="button" variant="outline" onClick={intentarCerrar} disabled={ocupado} className="h-10 rounded-xl px-4">
              Cerrar ventana
            </Button>
            {!cerrada && (
              <Button type="button" onClick={() => void guardar()} disabled={ocupado} className="h-10 rounded-xl bg-[#ffd400] px-4 font-bold text-zinc-950 hover:bg-[#f2c900]">
                {guardando ? <LoaderCircle className="animate-spin" aria-hidden="true" /> : <Save aria-hidden="true" />}
                Guardar ficha
              </Button>
            )}
          </div>
          {!cerrada && (
            <p className="flex items-center gap-1.5 text-[11px] text-zinc-400">
              <Wrench className="size-3.5" aria-hidden="true" />
              El vehículo seguirá en mantenimiento hasta que cierres la incidencia.
            </p>
          )}
        </div>
      )}
    </VentanaModal>
  );
}
