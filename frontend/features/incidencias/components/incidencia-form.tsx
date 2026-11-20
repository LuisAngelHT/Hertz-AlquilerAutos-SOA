"use client";

import { useState, type FormEvent } from "react";
import { AlertTriangle, LoaderCircle, Plus } from "lucide-react";
import { AutoDismissFeedback } from "@/components/auto-dismiss-feedback";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import type { Sesion } from "@/features/flota/lib/session";
import {
  registrarIncidencia,
  mensajeErrorIncidencia,
} from "@/features/incidencias/lib/incidencias-api";
import {
  TIPOS_INCIDENCIA,
  type NuevaIncidencia,
  type RegistroIncidencia,
  type TipoIncidencia,
} from "@/features/incidencias/types/incidencia";
import { ApiRequestError } from "@/lib/api-client";

const CAMPO =
  "h-11 w-full rounded-xl border border-zinc-200 bg-zinc-50/60 px-3 text-sm text-zinc-900 outline-none transition-colors focus-visible:border-amber-400 focus-visible:ring-3 focus-visible:ring-amber-300/40 disabled:opacity-50";

interface IncidenciaFormProps {
  sesion: Sesion;
  onCreada: (registro: RegistroIncidencia) => void;
  onSesionExpirada: () => void;
}

export function IncidenciaForm({
  sesion,
  onCreada,
  onSesionExpirada,
}: IncidenciaFormProps) {
  const [placa, setPlaca] = useState("");
  const [contrato, setContrato] = useState("");
  const [tipo, setTipo] = useState<TipoIncidencia>("DAÑO");
  const [descripcion, setDescripcion] = useState("");
  const [costo, setCosto] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [guardando, setGuardando] = useState(false);

  async function enviar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setGuardando(true);

    const request: NuevaIncidencia = {
      idVehiculo: placa.trim().toUpperCase(),
      idContrato: contrato.trim() || null,
      tipo,
      descripcion: descripcion.trim(),
      costoEstimado: costo.trim() ? Number(costo) : null,
    };

    try {
      const resultado = await registrarIncidencia(sesion.token, request);
      onCreada(resultado);
      setPlaca("");
      setContrato("");
      setTipo("DAÑO");
      setDescripcion("");
      setCosto("");
    } catch (cause) {
      if (cause instanceof ApiRequestError && cause.status === 401) {
        onSesionExpirada();
        return;
      }
      setError(mensajeErrorIncidencia(cause));
    } finally {
      setGuardando(false);
    }
  }

  return (
    <Card className="rounded-3xl border-0 bg-white shadow-[0_18px_60px_-32px_rgba(24,24,27,0.28)] ring-1 ring-zinc-200/80">
      <CardContent className="p-5 sm:p-7">
        <div className="mb-6 flex items-start gap-3">
          <div className="grid size-11 shrink-0 place-items-center rounded-xl bg-[#fff4b8] text-zinc-900">
            <AlertTriangle className="size-5" aria-hidden="true" />
          </div>
          <div>
            <h2 className="text-lg font-semibold tracking-tight text-zinc-950">
              Registrar incidencia
            </h2>
            <p className="mt-1 text-sm leading-5 text-zinc-500">
              Al registrar una incidencia, el vehículo se enviará a mantenimiento.
            </p>
          </div>
        </div>

        <form onSubmit={enviar} className="space-y-5">
          <div className="grid gap-5 sm:grid-cols-2">
            <div className="space-y-2">
              <label htmlFor="inc-placa" className="text-sm font-semibold text-zinc-800">
                Placa del vehículo
              </label>
              <Input
                id="inc-placa"
                value={placa}
                onChange={(event) => setPlaca(event.target.value.toUpperCase())}
                required
                maxLength={8}
                pattern="[A-Za-z0-9]{3}-[A-Za-z0-9]{3,4}"
                title="Formato esperado: ABC-123 o ABC-1234."
                placeholder="ABC-123"
                disabled={guardando}
                autoComplete="off"
                className={CAMPO}
              />
            </div>

            <div className="space-y-2">
              <label htmlFor="inc-tipo" className="text-sm font-semibold text-zinc-800">
                Tipo de incidencia
              </label>
              <select
                id="inc-tipo"
                value={tipo}
                onChange={(event) => setTipo(event.target.value as TipoIncidencia)}
                disabled={guardando}
                className={CAMPO}
              >
                {TIPOS_INCIDENCIA.map((opcion) => (
                  <option key={opcion.value} value={opcion.value}>
                    {opcion.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="space-y-2">
              <label htmlFor="inc-contrato" className="text-sm font-semibold text-zinc-800">
                ID de contrato <span className="font-normal text-zinc-400">(opcional)</span>
              </label>
              <Input
                id="inc-contrato"
                value={contrato}
                onChange={(event) => setContrato(event.target.value)}
                pattern="^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
                title="Ingresa un UUID válido."
                placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
                disabled={guardando}
                className={CAMPO}
              />
            </div>

            <div className="space-y-2">
              <label htmlFor="inc-costo" className="text-sm font-semibold text-zinc-800">
                Costo estimado en soles <span className="font-normal text-zinc-400">(opcional)</span>
              </label>
              <Input
                id="inc-costo"
                type="number"
                min="0"
                max="99999999.99"
                step="0.01"
                value={costo}
                onChange={(event) => setCosto(event.target.value)}
                placeholder="0.00"
                disabled={guardando}
                className={CAMPO}
              />
            </div>
          </div>

          <div className="space-y-2">
            <label htmlFor="inc-descripcion" className="text-sm font-semibold text-zinc-800">
              Descripción
            </label>
            <textarea
              id="inc-descripcion"
              value={descripcion}
              onChange={(event) => setDescripcion(event.target.value)}
              required
              maxLength={500}
              rows={4}
              placeholder="Describe qué ocurrió o qué reparación se requiere."
              disabled={guardando}
              className={`${CAMPO} h-auto min-h-28 resize-y py-3`}
            />
            <p className="text-right text-xs text-zinc-400">
              {descripcion.length}/500
            </p>
          </div>

          {error && (
            <AutoDismissFeedback key={error} role="alert" className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm leading-5 text-red-700">
              {error}
            </AutoDismissFeedback>
          )}

          <div className="flex justify-end border-t border-zinc-100 pt-5">
            <Button
              type="submit"
              disabled={guardando}
              className="h-11 w-full rounded-xl bg-[#ffd400] px-5 font-semibold text-zinc-950 hover:bg-[#f2c900] sm:w-auto"
            >
              {guardando ? (
                <>
                  <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
                  Registrando...
                </>
              ) : (
                <>
                  <Plus className="size-4" aria-hidden="true" />
                  Registrar incidencia
                </>
              )}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}
