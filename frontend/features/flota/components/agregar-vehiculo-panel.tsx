"use client";

import { useState, type FormEvent } from "react";
import { CirclePlus, LoaderCircle } from "lucide-react";
import { AutoDismissFeedback } from "@/components/auto-dismiss-feedback";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { CATEGORIAS, SUCURSALES, etiquetaCategoria, etiquetaSucursal } from "@/features/flota/lib/constants";
import { crearVehiculo, SesionExpiradaError, type Vehiculo } from "@/features/flota/lib/flota-api";
import type { Sesion } from "@/features/flota/lib/session";

const CAMPO =
  "h-12 w-full rounded-xl border border-zinc-200 bg-zinc-50/60 px-4 text-sm text-zinc-900 outline-none transition-colors focus-visible:border-amber-400 focus-visible:ring-3 focus-visible:ring-amber-300/40 disabled:opacity-50";

interface AgregarVehiculoPanelProps {
  sesion: Sesion;
  onSesionExpirada: () => void;
}

/**
 * Alta de un vehículo nuevo en la flota (la empresa adquirió una unidad). Solo la ve el
 * ADMINISTRADOR general; el backend lo valida igual. El vehículo nace DISPONIBLE.
 */
export function AgregarVehiculoPanel({ sesion, onSesionExpirada }: AgregarVehiculoPanelProps) {
  const anioMaximo = new Date().getFullYear() + 1;

  const [placa, setPlaca] = useState("");
  const [categoria, setCategoria] = useState<string>(CATEGORIAS[0].value);
  const [marca, setMarca] = useState("");
  const [modelo, setModelo] = useState("");
  const [anio, setAnio] = useState(String(new Date().getFullYear()));
  const [sucursal, setSucursal] = useState<string>(SUCURSALES[0].value);

  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [creado, setCreado] = useState<Vehiculo | null>(null);

  async function enviar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setEnviando(true);
    setError(null);
    setCreado(null);
    try {
      const vehiculo = await crearVehiculo(sesion, {
        placa: placa.trim().toUpperCase(),
        categoria,
        marca: marca.trim(),
        modelo: modelo.trim(),
        anio: Number(anio),
        sucursal,
      });
      setCreado(vehiculo);
      setPlaca("");
      setMarca("");
      setModelo("");
    } catch (cause) {
      if (cause instanceof SesionExpiradaError) {
        onSesionExpirada();
        return;
      }
      setError(cause instanceof Error ? cause.message : "No se pudo registrar el vehículo.");
    } finally {
      setEnviando(false);
    }
  }

  return (
    <section aria-labelledby="titulo-agregar" className="max-w-3xl">
      <Card className="rounded-3xl border-0 bg-white shadow-[0_18px_60px_-32px_rgba(24,24,27,0.28)] ring-1 ring-zinc-200/80">
        <CardContent className="p-5 sm:p-7">
          <h2 id="titulo-agregar" className="text-lg font-semibold tracking-tight text-zinc-950">
            Agregar un vehículo a la flota
          </h2>
          <p className="mt-1 text-sm text-zinc-500">
            Registra una unidad nueva. Queda disponible en la sucursal que elijas y el alta se guarda en su historial.
          </p>

          <form onSubmit={enviar} className="mt-6 space-y-5">
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <label htmlFor="veh-placa" className="text-sm font-semibold text-zinc-800">
                  Placa
                </label>
                <Input
                  id="veh-placa"
                  value={placa}
                  onChange={(e) => setPlaca(e.target.value.toUpperCase())}
                  placeholder="ABC-123"
                  required
                  maxLength={10}
                  pattern="[A-Za-z0-9]{3}-[A-Za-z0-9]{3,4}"
                  title="Formato ABC-123: 3 letras o números, guion y 3 o 4 más."
                  autoComplete="off"
                  className="h-12 rounded-xl border-zinc-200 bg-zinc-50/60 px-4 text-sm"
                />
              </div>
              <div className="space-y-2">
                <label htmlFor="veh-categoria" className="text-sm font-semibold text-zinc-800">
                  Categoría
                </label>
                <select id="veh-categoria" value={categoria} onChange={(e) => setCategoria(e.target.value)} className={CAMPO}>
                  {CATEGORIAS.map((c) => (
                    <option key={c.value} value={c.value}>
                      {c.label}
                    </option>
                  ))}
                </select>
              </div>
              <div className="space-y-2">
                <label htmlFor="veh-marca" className="text-sm font-semibold text-zinc-800">
                  Marca
                </label>
                <Input
                  id="veh-marca"
                  value={marca}
                  onChange={(e) => setMarca(e.target.value)}
                  placeholder="Toyota"
                  required
                  maxLength={40}
                  className="h-12 rounded-xl border-zinc-200 bg-zinc-50/60 px-4 text-sm"
                />
              </div>
              <div className="space-y-2">
                <label htmlFor="veh-modelo" className="text-sm font-semibold text-zinc-800">
                  Modelo
                </label>
                <Input
                  id="veh-modelo"
                  value={modelo}
                  onChange={(e) => setModelo(e.target.value)}
                  placeholder="Hilux"
                  required
                  maxLength={40}
                  className="h-12 rounded-xl border-zinc-200 bg-zinc-50/60 px-4 text-sm"
                />
              </div>
              <div className="space-y-2">
                <label htmlFor="veh-anio" className="text-sm font-semibold text-zinc-800">
                  Año
                </label>
                <Input
                  id="veh-anio"
                  type="number"
                  value={anio}
                  onChange={(e) => setAnio(e.target.value)}
                  min={2000}
                  max={anioMaximo}
                  required
                  className="h-12 rounded-xl border-zinc-200 bg-zinc-50/60 px-4 text-sm"
                />
              </div>
              <div className="space-y-2">
                <label htmlFor="veh-sucursal" className="text-sm font-semibold text-zinc-800">
                  Sucursal donde queda
                </label>
                <select id="veh-sucursal" value={sucursal} onChange={(e) => setSucursal(e.target.value)} className={CAMPO}>
                  {SUCURSALES.map((s) => (
                    <option key={s.value} value={s.value}>
                      {s.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {error && (
              <AutoDismissFeedback key={error} role="alert" className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm leading-5 text-red-700">
                {error}
              </AutoDismissFeedback>
            )}

            {creado && (
              <AutoDismissFeedback key={creado.placa} role="status" className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm leading-5 text-emerald-800">
                <p className="font-semibold">Vehículo registrado</p>
                <p className="mt-1">
                  {creado.marca} {creado.modelo} {creado.anio} · placa {creado.placa} · {etiquetaCategoria(creado.categoria)} ·{" "}
                  {etiquetaSucursal(creado.sucursal)}. Ya aparece como disponible.
                </p>
              </AutoDismissFeedback>
            )}

            <Button
              type="submit"
              disabled={enviando}
              className="h-12 rounded-xl bg-[#ffd400] px-7 text-sm font-bold text-zinc-950 shadow-[0_7px_18px_-9px_rgba(234,179,8,0.8)] hover:bg-[#f2c900]"
            >
              {enviando ? (
                <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
              ) : (
                <CirclePlus className="size-4" aria-hidden="true" />
              )}
              Registrar vehículo
            </Button>
          </form>
        </CardContent>
      </Card>
    </section>
  );
}
