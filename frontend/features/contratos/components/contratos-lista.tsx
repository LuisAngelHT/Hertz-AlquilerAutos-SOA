"use client";

import { useEffect, useState, type FormEvent } from "react";
import {
  ChevronLeft,
  ChevronRight,
  LoaderCircle,
  RefreshCw,
  Search,
  X,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import type { Sesion } from "@/features/flota/lib/session";
import {
  listarContratos,
  mensajeErrorContrato,
} from "@/features/contratos/lib/contratos-api";
import type {
  EstadoContrato,
  FiltroContratos,
  PaginaContratos,
} from "@/features/contratos/types/contrato";
import { ApiRequestError } from "@/lib/api-client";
import { ContratoCard } from "@/features/contratos/components/contrato-card";

const POR_PAGINA = 8;
const UUID_PATTERN =
  "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

interface ContratosListaProps {
  sesion: Sesion;
  onSesionExpirada: () => void;
}

export function ContratosLista({
  sesion,
  onSesionExpirada,
}: ContratosListaProps) {
  const [placa, setPlaca] = useState("");
  const [idReserva, setIdReserva] = useState("");
  const [idCliente, setIdCliente] = useState("");
  const [estado, setEstado] = useState<EstadoContrato | "">("");
  const [filtro, setFiltro] = useState<Omit<FiltroContratos, "pagina" | "tamano">>({});
  const [pagina, setPagina] = useState(0);
  const [revision, setRevision] = useState(0);
  const [resultado, setResultado] = useState<PaginaContratos | null>(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let activa = true;

    listarContratos(sesion.token, {
      ...filtro,
      pagina,
      tamano: POR_PAGINA,
    })
      .then((datos) => {
        if (!activa) return;
        setResultado(datos);
      })
      .catch((cause: unknown) => {
        if (!activa) return;
        if (cause instanceof ApiRequestError && cause.status === 401) {
          onSesionExpirada();
          return;
        }
        setResultado(null);
        setError(mensajeErrorContrato(cause));
      })
      .finally(() => {
        if (activa) setCargando(false);
      });

    return () => {
      activa = false;
    };
  }, [sesion.token, filtro, pagina, revision, onSesionExpirada]);

  function buscar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setCargando(true);
    setError(null);
    setPagina(0);
    setFiltro({
      placa: placa.trim().toUpperCase() || undefined,
      idReserva: idReserva.trim() || undefined,
      idCliente: idCliente.trim() || undefined,
      estado: estado || undefined,
    });
    setRevision((actual) => actual + 1);
  }

  function limpiarFiltros() {
    setCargando(true);
    setError(null);
    setPlaca("");
    setIdReserva("");
    setIdCliente("");
    setEstado("");
    setPagina(0);
    setFiltro({});
    setRevision((actual) => actual + 1);
  }

  function cambiarPagina(nuevaPagina: number) {
    setCargando(true);
    setError(null);
    setPagina(nuevaPagina);
  }

  function actualizar() {
    setCargando(true);
    setError(null);
    setRevision((actual) => actual + 1);
  }

  const desde =
    resultado && resultado.total > 0
      ? resultado.pagina * resultado.tamano + 1
      : 0;
  const hasta = resultado
    ? resultado.pagina * resultado.tamano + resultado.contratos.length
    : 0;

  return (
    <section aria-labelledby="titulo-lista-contratos" className="space-y-5">
      <Card className="rounded-3xl border-0 bg-white shadow-[0_18px_60px_-32px_rgba(24,24,27,0.28)] ring-1 ring-zinc-200/80">
        <CardContent className="p-5 sm:p-7">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h2 id="titulo-lista-contratos" className="text-lg font-semibold tracking-tight text-zinc-950">
                Contratos registrados
              </h2>
              <p className="mt-1 text-sm text-zinc-500">
                Filtra por vehículo, reserva, cliente o estado.
              </p>
            </div>
            <Button
              type="button"
              variant="outline"
              disabled={cargando}
              onClick={actualizar}
              className="h-10 rounded-xl"
            >
              {cargando ? (
                <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
              ) : (
                <RefreshCw className="size-4" aria-hidden="true" />
              )}
              Actualizar
            </Button>
          </div>

          <form onSubmit={buscar} className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <div className="space-y-2">
              <label htmlFor="filtro-contrato-placa" className="text-sm font-semibold text-zinc-800">
                Placa
              </label>
              <Input
                id="filtro-contrato-placa"
                value={placa}
                onChange={(event) => setPlaca(event.target.value.toUpperCase())}
                placeholder="Todas las placas"
                maxLength={10}
                disabled={cargando}
                className="h-11 rounded-xl border-zinc-200 bg-zinc-50/60 px-3 text-sm"
              />
            </div>

            <div className="space-y-2">
              <label htmlFor="filtro-contrato-reserva" className="text-sm font-semibold text-zinc-800">
                ID de reserva
              </label>
              <Input
                id="filtro-contrato-reserva"
                value={idReserva}
                onChange={(event) => setIdReserva(event.target.value)}
                pattern={UUID_PATTERN}
                title="Ingresa un UUID válido."
                placeholder="UUID de la reserva"
                disabled={cargando}
                className="h-11 rounded-xl border-zinc-200 bg-zinc-50/60 px-3 font-mono text-xs"
              />
            </div>

            <div className="space-y-2">
              <label htmlFor="filtro-contrato-cliente" className="text-sm font-semibold text-zinc-800">
                ID de cliente
              </label>
              <Input
                id="filtro-contrato-cliente"
                value={idCliente}
                onChange={(event) => setIdCliente(event.target.value)}
                pattern={UUID_PATTERN}
                title="Ingresa un UUID válido."
                placeholder="UUID del cliente"
                disabled={cargando}
                className="h-11 rounded-xl border-zinc-200 bg-zinc-50/60 px-3 font-mono text-xs"
              />
            </div>

            <div className="space-y-2">
              <label htmlFor="filtro-contrato-estado" className="text-sm font-semibold text-zinc-800">
                Estado
              </label>
              <select
                id="filtro-contrato-estado"
                value={estado}
                onChange={(event) => setEstado(event.target.value as EstadoContrato | "")}
                disabled={cargando}
                className="h-11 w-full rounded-xl border border-zinc-200 bg-zinc-50/60 px-3 text-sm text-zinc-900 outline-none focus-visible:border-amber-400 focus-visible:ring-3 focus-visible:ring-amber-300/40"
              >
                <option value="">Todos</option>
                <option value="VIGENTE">Vigentes</option>
                <option value="FINALIZADO">Finalizados</option>
              </select>
            </div>

            <div className="flex flex-wrap gap-2 sm:col-span-2 lg:col-span-4">
              <Button
                type="submit"
                disabled={cargando}
                className="h-11 rounded-xl bg-[#ffd400] px-5 font-semibold text-zinc-950 hover:bg-[#f2c900]"
              >
                {cargando ? (
                  <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
                ) : (
                  <Search className="size-4" aria-hidden="true" />
                )}
                Filtrar
              </Button>
              <Button
                type="button"
                variant="outline"
                disabled={cargando}
                onClick={limpiarFiltros}
                className="h-11 rounded-xl px-5"
              >
                <X className="size-4" aria-hidden="true" />
                Limpiar
              </Button>
            </div>
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
          Cargando contratos...
        </p>
      ) : resultado && resultado.contratos.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-zinc-300 bg-white px-5 py-12 text-center">
          <p className="font-medium text-zinc-800">No hay contratos para mostrar</p>
          <p className="mt-1 text-sm text-zinc-500">
            Prueba otros filtros. Los contratos se crean al entregar un vehículo desde Reservas.
          </p>
        </div>
      ) : (
        resultado && (
          <>
            <div className="flex flex-wrap items-center justify-between gap-2 text-sm text-zinc-500">
              <p>{desde}–{hasta} de {resultado.total} contratos</p>
              <p>
                Página {resultado.totalPaginas === 0 ? 0 : resultado.pagina + 1} de {resultado.totalPaginas}
              </p>
            </div>
            <div className="space-y-4">
              {resultado.contratos.map((contrato) => (
                <ContratoCard key={contrato.idContrato} contrato={contrato} />
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
