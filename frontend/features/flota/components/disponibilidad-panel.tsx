"use client";

import { useState, type FormEvent } from "react";
import { CalendarDays, LoaderCircle, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { VehiculoCard } from "@/features/flota/components/vehiculo-card";
import { CATEGORIAS, SUCURSALES } from "@/features/flota/lib/constants";
import {
  consultarDisponibilidad,
  SesionExpiradaError,
  type Disponibilidad,
  type Vehiculo,
} from "@/features/flota/lib/flota-api";
import type { Sesion } from "@/features/flota/lib/session";

// Fecha local en formato AAAA-MM-DD (el que espera la API).
function fechaISO(fecha: Date): string {
  const mes = String(fecha.getMonth() + 1).padStart(2, "0");
  const dia = String(fecha.getDate()).padStart(2, "0");
  return `${fecha.getFullYear()}-${mes}-${dia}`;
}

function sumarDias(fecha: Date, dias: number): Date {
  const copia = new Date(fecha);
  copia.setDate(copia.getDate() + dias);
  return copia;
}

// Cuántos vehículos se muestran de entrada; el resto aparece con "Mostrar más".
const POR_TANDA = 12;

const CAMPO =
  "h-12 w-full rounded-xl border border-zinc-200 bg-zinc-50/60 px-4 text-sm text-zinc-900 outline-none transition-colors focus-visible:border-amber-400 focus-visible:ring-3 focus-visible:ring-amber-300/40 disabled:opacity-50";

interface DisponibilidadPanelProps {
  sesion: Sesion;
  puedeEditar: boolean;
  onSesionExpirada: () => void;
}

export function DisponibilidadPanel({
  sesion,
  puedeEditar,
  onSesionExpirada,
}: DisponibilidadPanelProps) {
  const hoy = new Date();
  const [sucursal, setSucursal] = useState<string>(SUCURSALES[0].value);
  const [categoria, setCategoria] = useState<string>(CATEGORIAS[0].value);
  const [fechaInicio, setFechaInicio] = useState(fechaISO(hoy));
  const [fechaFin, setFechaFin] = useState(fechaISO(sumarDias(hoy, 3)));
  const [buscando, setBuscando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [resultado, setResultado] = useState<Disponibilidad | null>(null);
  const [visibles, setVisibles] = useState(POR_TANDA);

  async function buscar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);

    if (fechaFin < fechaInicio) {
      setError("La fecha de fin no puede ser anterior a la fecha de inicio.");
      return;
    }

    setBuscando(true);
    setVisibles(POR_TANDA);
    try {
      setResultado(
        await consultarDisponibilidad(sesion, {
          sucursal,
          categoria,
          fechaInicio,
          fechaFin,
        }),
      );
    } catch (cause) {
      if (cause instanceof SesionExpiradaError) {
        onSesionExpirada();
        return;
      }
      setResultado(null);
      setError(cause instanceof Error ? cause.message : "No se pudo consultar la disponibilidad.");
    } finally {
      setBuscando(false);
    }
  }

  // Cuando un vehículo cambia de estado se reemplaza en la lista, para que se vea el cambio.
  function reemplazar(actualizado: Vehiculo) {
    setResultado((actual) =>
      actual
        ? {
            ...actual,
            vehiculos: actual.vehiculos.map((v) =>
              v.placa === actualizado.placa ? actualizado : v,
            ),
          }
        : actual,
    );
  }

  return (
    <section aria-labelledby="titulo-disponibilidad" className="space-y-5">
      <Card className="rounded-3xl border-0 bg-white shadow-[0_18px_60px_-32px_rgba(24,24,27,0.28)] ring-1 ring-zinc-200/80">
        <CardContent className="p-5 sm:p-7">
          <div className="mb-5 flex items-center gap-3">
            <span className="grid size-10 place-items-center rounded-xl bg-[#fff4b8] text-zinc-950">
              <CalendarDays className="size-5" aria-hidden="true" />
            </span>
            <div>
              <h2
                id="titulo-disponibilidad"
                className="text-lg font-semibold tracking-tight text-zinc-950"
              >
                Consultar disponibilidad
              </h2>
              <p className="text-sm text-zinc-500">
                Vehículos libres en toda la fecha elegida.
              </p>
            </div>
          </div>

          <form onSubmit={buscar} className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <label htmlFor="sucursal" className="text-sm font-semibold text-zinc-800">
                Sucursal
              </label>
              <select
                id="sucursal"
                value={sucursal}
                onChange={(e) => setSucursal(e.target.value)}
                disabled={buscando}
                className={CAMPO}
              >
                {SUCURSALES.map((s) => (
                  <option key={s.value} value={s.value}>
                    {s.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="space-y-2">
              <label htmlFor="categoria" className="text-sm font-semibold text-zinc-800">
                Categoría
              </label>
              <select
                id="categoria"
                value={categoria}
                onChange={(e) => setCategoria(e.target.value)}
                disabled={buscando}
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
              <label htmlFor="fechaInicio" className="text-sm font-semibold text-zinc-800">
                Desde
              </label>
              <Input
                id="fechaInicio"
                type="date"
                value={fechaInicio}
                onChange={(e) => setFechaInicio(e.target.value)}
                required
                disabled={buscando}
                className="h-12 rounded-xl border-zinc-200 bg-zinc-50/60 px-4 text-sm focus-visible:border-amber-400 focus-visible:ring-amber-300/40"
              />
            </div>

            <div className="space-y-2">
              <label htmlFor="fechaFin" className="text-sm font-semibold text-zinc-800">
                Hasta
              </label>
              <Input
                id="fechaFin"
                type="date"
                value={fechaFin}
                min={fechaInicio}
                onChange={(e) => setFechaFin(e.target.value)}
                required
                disabled={buscando}
                className="h-12 rounded-xl border-zinc-200 bg-zinc-50/60 px-4 text-sm focus-visible:border-amber-400 focus-visible:ring-amber-300/40"
              />
            </div>

            <Button
              type="submit"
              disabled={buscando}
              className="h-12 rounded-xl bg-[#ffd400] text-sm font-bold text-zinc-950 shadow-[0_7px_18px_-9px_rgba(234,179,8,0.8)] hover:bg-[#f2c900] sm:col-span-2"
            >
              {buscando ? (
                <>
                  <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
                  Buscando...
                </>
              ) : (
                <>
                  <Search className="size-4" aria-hidden="true" />
                  Buscar vehículos
                </>
              )}
            </Button>
          </form>

          {error && (
            <div
              role="alert"
              className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm leading-5 text-red-700"
            >
              {error}
            </div>
          )}
        </CardContent>
      </Card>

      {resultado && (
        <div className="space-y-4" aria-live="polite">
          <p className="text-sm font-medium text-zinc-600">
            {resultado.cantidadDisponible === 0
              ? "No hay vehículos disponibles con esos filtros."
              : `${resultado.cantidadDisponible} ${
                  resultado.cantidadDisponible === 1
                    ? "vehículo disponible"
                    : "vehículos disponibles"
                }`}
          </p>
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {resultado.vehiculos.slice(0, visibles).map((vehiculo) => (
              <VehiculoCard
                key={vehiculo.placa}
                vehiculo={vehiculo}
                sesion={sesion}
                puedeEditar={puedeEditar}
                onActualizado={reemplazar}
                onSesionExpirada={onSesionExpirada}
              />
            ))}
          </div>
          {resultado.vehiculos.length > visibles && (
            <div className="flex justify-center">
              <Button
                type="button"
                variant="outline"
                onClick={() => setVisibles((v) => v + POR_TANDA)}
                className="h-10 rounded-xl border-zinc-200 px-5"
              >
                Mostrar más ({resultado.vehiculos.length - visibles} restantes)
              </Button>
            </div>
          )}
        </div>
      )}
    </section>
  );
}
