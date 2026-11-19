"use client";

import { useEffect, useState, type FormEvent } from "react";
import { ChevronLeft, ChevronRight, LoaderCircle, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import type { Sesion } from "@/features/flota/lib/session";
import {
  listarIncidencias,
  mensajeErrorIncidencia,
} from "@/features/incidencias/lib/incidencias-api";
import {
  type EstadoIncidencia,
  type PaginaIncidencias,
} from "@/features/incidencias/types/incidencia";
import { ApiRequestError } from "@/lib/api-client";
import { IncidenciaCard } from "@/features/incidencias/components/incidencia-card";

const POR_PAGINA = 8;

interface IncidenciasListaProps {
  sesion: Sesion;
  version: number;
  onSesionExpirada: () => void;
}

export function IncidenciasLista({
  sesion,
  version,
  onSesionExpirada,
}: IncidenciasListaProps) {
  const [placa, setPlaca] = useState("");
  const [estado, setEstado] = useState<EstadoIncidencia | "">("");
  const [filtro, setFiltro] = useState({ placa: "", estado: "" as EstadoIncidencia | "" });
  const [pagina, setPagina] = useState(0);
  const [resultado, setResultado] = useState<PaginaIncidencias | null>(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let activa = true;
    listarIncidencias(sesion.token, {
      idVehiculo: filtro.placa,
      estado: filtro.estado || undefined,
      pagina,
      tamano: POR_PAGINA,
    })
      .then((datos) => {
        if (!activa) return;
        setResultado(datos);
        setError(null);
      })
      .catch((cause: unknown) => {
        if (!activa) return;
        if (cause instanceof ApiRequestError && cause.status === 401) {
          onSesionExpirada();
          return;
        }
        setResultado(null);
        setError(mensajeErrorIncidencia(cause));
      })
      .finally(() => {
        if (activa) setCargando(false);
      });

    return () => {
      activa = false;
    };
  }, [sesion.token, filtro, pagina, version, onSesionExpirada]);

  function buscar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setCargando(true);
    setPagina(0);
    setFiltro({ placa: placa.trim().toUpperCase(), estado });
  }

  function cambiarPagina(nuevaPagina: number) {
    setCargando(true);
    setPagina(nuevaPagina);
  }

  const desde =
    resultado && resultado.total > 0
      ? resultado.pagina * resultado.tamano + 1
      : 0;
  const hasta = resultado
    ? resultado.pagina * resultado.tamano + resultado.incidencias.length
    : 0;

  return (
    <section aria-labelledby="titulo-lista-incidencias" className="space-y-5">
      <Card className="rounded-3xl border-0 bg-white shadow-[0_18px_60px_-32px_rgba(24,24,27,0.28)] ring-1 ring-zinc-200/80">
        <CardContent className="p-5 sm:p-7">
          <h2 id="titulo-lista-incidencias" className="text-lg font-semibold tracking-tight text-zinc-950">
            Incidencias registradas
          </h2>
          <p className="mt-1 text-sm text-zinc-500">
            Consulta las incidencias de los vehículos accesibles para tu sucursal.
          </p>
          <form onSubmit={buscar} className="mt-5 grid gap-4 sm:grid-cols-[1fr_1fr_auto] sm:items-end">
            <div className="space-y-2">
              <label htmlFor="filtro-inc-placa" className="text-sm font-semibold text-zinc-800">
                Placa
              </label>
              <Input
                id="filtro-inc-placa"
                value={placa}
                onChange={(event) => setPlaca(event.target.value.toUpperCase())}
                placeholder="Todas las placas"
                maxLength={8}
                disabled={cargando}
                className="h-11 rounded-xl border-zinc-200 bg-zinc-50/60 px-3 text-sm"
              />
            </div>
            <div className="space-y-2">
              <label htmlFor="filtro-inc-estado" className="text-sm font-semibold text-zinc-800">
                Estado
              </label>
              <select
                id="filtro-inc-estado"
                value={estado}
                onChange={(event) => setEstado(event.target.value as EstadoIncidencia | "")}
                disabled={cargando}
                className="h-11 w-full rounded-xl border border-zinc-200 bg-zinc-50/60 px-3 text-sm text-zinc-900 outline-none focus-visible:border-amber-400 focus-visible:ring-3 focus-visible:ring-amber-300/40"
              >
                <option value="">Todos</option>
                <option value="ABIERTA">Abiertas</option>
                <option value="CERRADA">Cerradas</option>
              </select>
            </div>
            <Button
              type="submit"
              disabled={cargando}
              variant="outline"
              className="h-11 rounded-xl border-zinc-200 px-5"
            >
              {cargando ? (
                <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
              ) : (
                <Search className="size-4" aria-hidden="true" />
              )}
              Filtrar
            </Button>
          </form>
        </CardContent>
      </Card>

      {error && (
        <div role="alert" className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm leading-5 text-red-700">
          {error}
        </div>
      )}

      {cargando && !resultado ? (
        <p className="flex items-center justify-center gap-2 py-10 text-sm text-zinc-500">
          <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
          Cargando incidencias...
        </p>
      ) : resultado && resultado.incidencias.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-zinc-300 bg-white px-5 py-12 text-center">
          <p className="font-medium text-zinc-800">No hay incidencias para mostrar</p>
          <p className="mt-1 text-sm text-zinc-500">Prueba otros filtros o registra una incidencia.</p>
        </div>
      ) : (
        resultado && (
          <>
            <div className="flex items-center justify-between text-sm text-zinc-500">
              <p>{desde}–{hasta} de {resultado.total} incidencias</p>
              <p>Página {resultado.totalPaginas === 0 ? 0 : resultado.pagina + 1} de {resultado.totalPaginas}</p>
            </div>
            <div className="space-y-4">
              {resultado.incidencias.map((incidencia) => (
                <IncidenciaCard key={incidencia.idIncidencia} incidencia={incidencia} />
              ))}
            </div>
            <div className="flex items-center justify-between gap-3">
              <Button
                type="button"
                variant="outline"
                disabled={cargando || pagina <= 0}
                onClick={() => cambiarPagina(pagina - 1)}
                className="h-10 rounded-xl"
              >
                <ChevronLeft className="size-4" aria-hidden="true" />
                Anterior
              </Button>
              <Button
                type="button"
                variant="outline"
                disabled={cargando || pagina + 1 >= resultado.totalPaginas}
                onClick={() => cambiarPagina(pagina + 1)}
                className="h-10 rounded-xl"
              >
                Siguiente
                <ChevronRight className="size-4" aria-hidden="true" />
              </Button>
            </div>
          </>
        )
      )}
    </section>
  );
}
