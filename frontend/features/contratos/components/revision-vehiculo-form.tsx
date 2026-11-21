"use client";

import { useState } from "react";
import { Plus, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  VehiculoVistas,
  type PinVista,
  type TipoCarroceria,
} from "@/features/contratos/components/vehiculo-diagrama";
import {
  COMBUSTIBLES,
  ESTADOS_ITEM,
  GRAVEDADES,
  GRUPOS_INVENTARIO,
  MAX_DANOS,
  TIPOS_DANO,
  ZONAS_SIN_DIBUJO,
  danoEnDibujo,
  danoSinUbicar,
  estadosDeRevision,
  etiquetaEstadoItem,
  etiquetaVista,
  formatoKm,
  formatoSoles,
  itemEmpeoro,
  totalACargo,
  type DanoForm,
  type RevisionForm,
} from "@/features/contratos/lib/revision";
import type {
  EstadoItem,
  GravedadDano,
  RevisionVehiculo,
  TipoDano,
  VistaVehiculo,
  ZonaVehiculo,
} from "@/features/contratos/types/contrato";

const CAMPO =
  "h-10 w-full rounded-xl border border-zinc-200 bg-white px-3 text-sm text-zinc-900 outline-none transition-colors focus-visible:border-amber-400 focus-visible:ring-3 focus-visible:ring-amber-300/40 disabled:opacity-50";

const LETRAS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

interface RevisionVehiculoFormProps {
  /** ENTREGA: como sale el vehículo (daños que ya tiene). DEVOLUCIÓN: como vuelve (daños nuevos, a cargo del cliente). */
  modo: "ENTREGA" | "DEVOLUCION";
  valor: RevisionForm;
  onCambio: (valor: RevisionForm) => void;
  /** Silueta del dibujo según la categoría del vehículo. */
  tipo: TipoCarroceria;
  /** En la devolución, la revisión de salida: sus daños salen en gris sobre el dibujo y de ahí se compara el inventario. */
  entrega?: RevisionVehiculo | null;
  /** En la devolución, el kilometraje de la entrega: no se puede devolver con menos. */
  kmMinimo?: number | null;
  deshabilitado?: boolean;
}

/** Revisión del vehículo: kilometraje, combustible, daños marcados en el dibujo e inventario de Antes y Después. */
export function RevisionVehiculoForm({
  modo,
  valor,
  onCambio,
  tipo,
  entrega = null,
  kmMinimo,
  deshabilitado = false,
}: RevisionVehiculoFormProps) {
  const esDevolucion = modo === "DEVOLUCION";
  const total = totalACargo(valor);
  const [seleccionado, setSeleccionado] = useState<number | null>(null);
  const salida = estadosDeRevision(entrega);

  // Marcas del dibujo: las de la entrega en gris con letras (ya estaban) y las de esta revisión numeradas.
  const previos: PinVista[] = esDevolucion
    ? (entrega?.danos ?? []).flatMap((d, i) =>
        d.vista && d.posX != null && d.posY != null
          ? [{ id: `previo-${i}`, vista: d.vista, x: d.posX, y: d.posY, numero: LETRAS[i % 26], tono: "previo" as const, titulo: `Ya estaba al salir: ${d.descripcion}` }]
          : [],
      )
    : [];
  const ubicados = valor.danos.filter((d) => d.vista);
  const nuevos: PinVista[] = valor.danos.flatMap((d, i) =>
    d.vista && d.x != null && d.y != null
      ? [
          {
            id: d.clave,
            vista: d.vista,
            x: d.x,
            y: d.y,
            numero: i + 1,
            tono: esDevolucion ? ("nuevo" as const) : ("previo" as const),
            seleccionado: d.clave === seleccionado,
            titulo: `Daño ${i + 1}`,
          },
        ]
      : [],
  );

  function cambiarDano(clave: number, cambios: Partial<DanoForm>) {
    onCambio({ ...valor, danos: valor.danos.map((d) => (d.clave === clave ? { ...d, ...cambios } : d)) });
  }

  function agregarEnDibujo(vista: VistaVehiculo, x: number, y: number) {
    if (deshabilitado || valor.danos.length >= MAX_DANOS) return;
    const dano = danoEnDibujo(vista, x, y);
    onCambio({ ...valor, danos: [...valor.danos, dano] });
    setSeleccionado(dano.clave);
  }

  function seleccionar(id: string | number) {
    if (typeof id !== "number") return;
    setSeleccionado(id);
    document.getElementById(`dano-${id}`)?.scrollIntoView({ block: "nearest", behavior: "smooth" });
  }

  function marcarItem(codigo: string, estado: EstadoItem) {
    const costos = { ...valor.costosInventario };
    if (estado === "OK") delete costos[codigo];
    onCambio({ ...valor, inventario: { ...valor.inventario, [codigo]: estado }, costosInventario: costos });
  }

  return (
    <fieldset disabled={deshabilitado} className="space-y-5">
      <div className="grid gap-4 sm:grid-cols-2">
        <label className="space-y-1.5 text-xs font-medium text-zinc-600">
          Kilometraje actual
          <input
            type="number"
            inputMode="numeric"
            min={kmMinimo ?? 0}
            max={2000000}
            step={1}
            value={valor.kilometraje}
            onChange={(e) => onCambio({ ...valor, kilometraje: e.target.value })}
            placeholder={kmMinimo != null ? `Mínimo ${kmMinimo}` : "Ej. 12450"}
            className={CAMPO}
          />
          {kmMinimo != null && (
            <span className="block text-[11px] font-normal text-zinc-500">Salió con {formatoKm(kmMinimo)}.</span>
          )}
        </label>

        <div className="space-y-1.5">
          <span id="etiqueta-combustible" className="text-xs font-medium text-zinc-600">
            Combustible
          </span>
          <div role="radiogroup" aria-labelledby="etiqueta-combustible" className="flex flex-wrap gap-1.5">
            {COMBUSTIBLES.map((opcion) => {
              const activo = valor.combustible === opcion.value;
              return (
                <button
                  key={opcion.value}
                  type="button"
                  role="radio"
                  aria-checked={activo}
                  onClick={() => onCambio({ ...valor, combustible: opcion.value })}
                  className={`h-10 min-w-12 rounded-xl px-3 text-sm font-semibold ring-1 transition-colors disabled:opacity-50 ${
                    activo
                      ? "bg-[#fff4b8] text-zinc-950 ring-2 ring-amber-400"
                      : "bg-white text-zinc-600 ring-zinc-200 hover:bg-zinc-50"
                  }`}
                >
                  {opcion.label}
                </button>
              );
            })}
          </div>
        </div>
      </div>

      {/* ------------------------------------------------------------ carrocería */}
      <div className="space-y-3 rounded-2xl bg-zinc-50/70 p-4 ring-1 ring-zinc-200/70">
        <div>
          <p className="text-sm font-semibold text-zinc-800">
            {esDevolucion ? "Daños nuevos (no los tenía al salir)" : "Daños que el vehículo ya tiene"}
          </p>
          <p className="mt-0.5 text-xs leading-5 text-zinc-500">
            <strong className="text-zinc-700">Haz clic en el dibujo</strong> donde está el golpe para marcarlo.{" "}
            {esDevolucion
              ? "Las marcas grises con letra ya estaban al salir; anota solo lo que NO estaba: lo asume el cliente."
              : "Lo que anotes aquí NO se le cobrará al cliente: queda como constancia de cómo salió."}
          </p>
        </div>

        <VehiculoVistas
          tipo={tipo}
          pines={[...previos, ...nuevos]}
          onColocar={deshabilitado ? undefined : agregarEnDibujo}
          onSeleccionar={seleccionar}
        />

        {valor.danos.length === 0 ? (
          <p className="rounded-xl bg-white px-3 py-2.5 text-sm text-zinc-600 ring-1 ring-zinc-200">
            {esDevolucion ? "Sin daños nuevos." : "Sin daños: el vehículo sale en perfecto estado."}
          </p>
        ) : (
          <ul className="space-y-2">
            {valor.danos.map((dano, indice) => {
              const activo = dano.clave === seleccionado;
              return (
                <li
                  key={dano.clave}
                  id={`dano-${dano.clave}`}
                  onClick={() => setSeleccionado(dano.clave)}
                  className={`space-y-2 rounded-xl bg-white p-3 ring-1 transition-shadow ${
                    activo ? "ring-2 ring-amber-400" : "ring-zinc-200"
                  }`}
                >
                  <div className="flex items-center justify-between gap-2">
                    <span className="flex items-center gap-2 text-xs font-semibold text-zinc-600">
                      <span
                        className={`grid size-6 place-items-center rounded-full text-[11px] font-bold text-white ${
                          dano.vista ? (esDevolucion ? "bg-red-600" : "bg-zinc-500") : "bg-zinc-400"
                        }`}
                      >
                        {indice + 1}
                      </span>
                      {dano.vista ? etiquetaVista(dano.vista) : "Sin ubicar en el dibujo"}
                    </span>
                    <button
                      type="button"
                      onClick={() => onCambio({ ...valor, danos: valor.danos.filter((d) => d.clave !== dano.clave) })}
                      aria-label={`Quitar el daño ${indice + 1}`}
                      className="rounded-md p-1 text-zinc-400 hover:bg-red-50 hover:text-red-600"
                    >
                      <Trash2 className="size-4" aria-hidden="true" />
                    </button>
                  </div>
                  <div className="grid gap-2 sm:grid-cols-3">
                    {!dano.vista && (
                      <select
                        value={dano.zona}
                        onChange={(e) => cambiarDano(dano.clave, { zona: e.target.value as ZonaVehiculo })}
                        aria-label={`Zona del daño ${indice + 1}`}
                        className={CAMPO}
                      >
                        {ZONAS_SIN_DIBUJO.map((z) => (
                          <option key={z.value} value={z.value}>
                            {z.label}
                          </option>
                        ))}
                      </select>
                    )}
                    <select
                      value={dano.tipo}
                      onChange={(e) => cambiarDano(dano.clave, { tipo: e.target.value as TipoDano })}
                      aria-label={`Tipo del daño ${indice + 1}`}
                      className={CAMPO}
                    >
                      {TIPOS_DANO.map((t) => (
                        <option key={t.value} value={t.value}>
                          {t.label}
                        </option>
                      ))}
                    </select>
                    <select
                      value={dano.gravedad}
                      onChange={(e) => cambiarDano(dano.clave, { gravedad: e.target.value as GravedadDano })}
                      aria-label={`Gravedad del daño ${indice + 1}`}
                      className={CAMPO}
                    >
                      {GRAVEDADES.map((g) => (
                        <option key={g.value} value={g.value}>
                          {g.label}
                        </option>
                      ))}
                    </select>
                  </div>
                  <input
                    value={dano.descripcion}
                    onChange={(e) => cambiarDano(dano.clave, { descripcion: e.target.value })}
                    maxLength={300}
                    placeholder="Detalle (opcional): ej. rayón de 10 cm junto a la placa"
                    aria-label={`Detalle del daño ${indice + 1}`}
                    className={CAMPO}
                  />
                  {esDevolucion && (
                    <label className="flex items-center gap-2 text-xs font-medium text-zinc-600">
                      Costo estimado de reparar (S/)
                      <input
                        type="number"
                        inputMode="decimal"
                        min={0}
                        step="0.01"
                        value={dano.costo}
                        onChange={(e) => cambiarDano(dano.clave, { costo: e.target.value })}
                        placeholder="Opcional"
                        className={`${CAMPO} max-w-36`}
                      />
                    </label>
                  )}
                </li>
              );
            })}
          </ul>
        )}

        <div className="flex flex-wrap items-center justify-between gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            disabled={valor.danos.length >= MAX_DANOS}
            onClick={() => {
              const dano = danoSinUbicar();
              onCambio({ ...valor, danos: [...valor.danos, dano] });
              setSeleccionado(dano.clave);
            }}
            className="rounded-xl"
          >
            <Plus aria-hidden="true" />
            Daño interior u otra zona (sin dibujo)
          </Button>
          <p className="text-xs text-zinc-500">{ubicados.length} marcado(s) en el dibujo · {valor.danos.length} en total</p>
        </div>
      </div>

      {/* ------------------------------------------------------------ inventario */}
      <div className="space-y-3 rounded-2xl bg-zinc-50/70 p-4 ring-1 ring-zinc-200/70">
        <div>
          <p className="text-sm font-semibold text-zinc-800">Inventario del vehículo</p>
          <p className="mt-0.5 text-xs leading-5 text-zinc-500">
            {esDevolucion
              ? "Parte de cómo salió. Cambia solo lo que falta o vuelve dañado: lo que ya faltaba al salir no se cobra."
              : "Todo parte como «Bien». Marca lo que falte o esté dañado antes de entregar, para que no se le cobre después."}
          </p>
        </div>
        <div className="grid gap-3 lg:grid-cols-2">
          {GRUPOS_INVENTARIO.map((grupo) => (
            <div key={grupo.grupo} className="space-y-1.5 rounded-xl bg-white p-3 ring-1 ring-zinc-200">
              <p className="text-xs font-semibold uppercase tracking-wide text-zinc-500">{grupo.grupo}</p>
              <ul className="divide-y divide-zinc-100">
                {grupo.items.map((item) => {
                  const estado = valor.inventario[item.codigo] ?? "OK";
                  const empeoro = esDevolucion && itemEmpeoro(item.codigo, valor, entrega);
                  return (
                    <li key={item.codigo} className="flex flex-wrap items-center justify-between gap-x-3 gap-y-1 py-1.5">
                      <span className="text-sm text-zinc-700">{item.etiqueta}</span>
                      <span className="flex flex-wrap items-center gap-1.5">
                        {esDevolucion && (
                          <span className="text-[11px] text-zinc-400">Salió: {etiquetaEstadoItem(salida[item.codigo] ?? "OK")}</span>
                        )}
                        <span role="radiogroup" aria-label={item.etiqueta} className="inline-flex overflow-hidden rounded-lg ring-1 ring-zinc-200">
                          {ESTADOS_ITEM.map((e) => {
                            const activo = estado === e.value;
                            const color =
                              e.value === "OK"
                                ? "bg-emerald-100 text-emerald-800"
                                : e.value === "FALTA"
                                  ? "bg-amber-100 text-amber-900"
                                  : "bg-red-100 text-red-800";
                            return (
                              <button
                                key={e.value}
                                type="button"
                                role="radio"
                                aria-checked={activo}
                                onClick={() => marcarItem(item.codigo, e.value)}
                                className={`px-2.5 py-1 text-xs font-semibold transition-colors ${
                                  activo ? color : "bg-white text-zinc-500 hover:bg-zinc-50"
                                }`}
                              >
                                {e.label}
                              </button>
                            );
                          })}
                        </span>
                      </span>
                      {empeoro && (
                        <label className="flex w-full items-center gap-2 text-[11px] font-medium text-red-700">
                          Costo estimado (S/)
                          <input
                            type="number"
                            inputMode="decimal"
                            min={0}
                            step="0.01"
                            value={valor.costosInventario[item.codigo] ?? ""}
                            onChange={(e) =>
                              onCambio({
                                ...valor,
                                costosInventario: { ...valor.costosInventario, [item.codigo]: e.target.value },
                              })
                            }
                            placeholder="Opcional"
                            className={`${CAMPO} h-8 max-w-32 text-xs`}
                          />
                          <span className="font-normal text-zinc-500">a cargo del cliente</span>
                        </label>
                      )}
                    </li>
                  );
                })}
              </ul>
            </div>
          ))}
        </div>
        {esDevolucion && total > 0 && (
          <p className="text-right text-sm font-semibold text-red-700">El cliente asume {formatoSoles(total)} en total</p>
        )}
      </div>

      <label className="block space-y-1.5 text-xs font-medium text-zinc-600">
        Observaciones (opcional)
        <textarea
          value={valor.observaciones}
          onChange={(e) => onCambio({ ...valor, observaciones: e.target.value })}
          maxLength={500}
          rows={2}
          placeholder={
            esDevolucion
              ? "Cualquier detalle de la devolución (limpieza, objetos olvidados, acuerdos con el cliente...)"
              : "Detalles a tener en cuenta al entregar el vehículo"
          }
          className="w-full rounded-xl border border-zinc-200 bg-white px-3 py-2 text-sm font-normal text-zinc-900 outline-none transition-colors focus-visible:border-amber-400 focus-visible:ring-3 focus-visible:ring-amber-300/40 disabled:opacity-50"
        />
      </label>
    </fieldset>
  );
}
