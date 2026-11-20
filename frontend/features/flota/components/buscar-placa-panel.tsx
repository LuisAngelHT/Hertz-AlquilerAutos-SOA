"use client";

import { useState, type FormEvent } from "react";
import { LoaderCircle, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { VehiculoCard } from "@/features/flota/components/vehiculo-card";
import {
  consultarVehiculo,
  SesionExpiradaError,
  type Vehiculo,
} from "@/features/flota/lib/flota-api";
import type { Sesion } from "@/features/flota/lib/session";

interface BuscarPlacaPanelProps {
  sesion: Sesion;
  puedeEditar: boolean;
  onSesionExpirada: () => void;
}

export function BuscarPlacaPanel({
  sesion,
  puedeEditar,
  onSesionExpirada,
}: BuscarPlacaPanelProps) {
  const [placa, setPlaca] = useState("");
  const [buscando, setBuscando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [vehiculo, setVehiculo] = useState<Vehiculo | null>(null);

  async function buscar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setBuscando(true);
    try {
      setVehiculo(await consultarVehiculo(sesion, placa));
    } catch (cause) {
      if (cause instanceof SesionExpiradaError) {
        onSesionExpirada();
        return;
      }
      setVehiculo(null);
      setError(cause instanceof Error ? cause.message : "No se pudo consultar el vehículo.");
    } finally {
      setBuscando(false);
    }
  }

  return (
    <section aria-labelledby="titulo-placa" className="space-y-5">
      <Card className="rounded-3xl border-0 bg-white shadow-[0_18px_60px_-32px_rgba(24,24,27,0.28)] ring-1 ring-zinc-200/80">
        <CardContent className="p-5 sm:p-7">
          <h2
            id="titulo-placa"
            className="text-lg font-semibold tracking-tight text-zinc-950"
          >
            Buscar por placa
          </h2>
          <p className="mt-1 text-sm text-zinc-500">
            Consulta un vehículo puntual, por ejemplo ABC-103.
          </p>

          <form onSubmit={buscar} className="mt-5 space-y-3">
            <label htmlFor="placa" className="sr-only">
              Placa del vehículo
            </label>
            <Input
              id="placa"
              value={placa}
              onChange={(e) => setPlaca(e.target.value.toUpperCase())}
              placeholder="ABC-103"
              required
              maxLength={10}
              disabled={buscando}
              autoComplete="off"
              className="h-12 rounded-xl border-zinc-200 bg-zinc-50/60 px-4 font-mono text-sm tracking-wide placeholder:text-zinc-400 focus-visible:border-amber-400 focus-visible:ring-amber-300/40"
            />
            <Button
              type="submit"
              disabled={buscando || placa.trim() === ""}
              variant="outline"
              className="h-12 w-full rounded-xl border-zinc-200 text-sm font-semibold"
            >
              {buscando ? (
                <>
                  <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
                  Buscando...
                </>
              ) : (
                <>
                  <Search className="size-4" aria-hidden="true" />
                  Buscar
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

      {vehiculo && (
        <div aria-live="polite">
          <VehiculoCard
            vehiculo={vehiculo}
            sesion={sesion}
            puedeEditar={puedeEditar}
            onActualizado={setVehiculo}
            onSesionExpirada={onSesionExpirada}
          />
        </div>
      )}
    </section>
  );
}
