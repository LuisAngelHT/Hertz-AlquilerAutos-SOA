"use client";

import { useEffect, useState, type FormEvent } from "react";
import { ChevronLeft, ChevronRight, LoaderCircle, Search } from "lucide-react";
import { AutoDismissFeedback } from "@/components/auto-dismiss-feedback";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { TIPOS_DOCUMENTO, type TipoDocumento } from "@/features/clientes/types/cliente";
import { SUCURSALES } from "@/features/flota/lib/constants";
import type { Sesion } from "@/features/flota/lib/session";
import { ReservaItem } from "@/features/reservas/components/reserva-item";
import { ESTADOS_RESERVA } from "@/features/reservas/lib/constants";
import {
  listarReservas,
  SesionExpiradaError,
  type EstadoReserva,
  type PaginaReservas,
  type Reserva,
} from "@/features/reservas/lib/reservas-api";

const CAMPO =
  "h-12 w-full rounded-xl border border-zinc-200 bg-zinc-50/60 px-4 text-sm text-zinc-900 outline-none transition-colors focus-visible:border-amber-400 focus-visible:ring-3 focus-visible:ring-amber-300/40 disabled:opacity-50";

// Reservas por página: con cientos de reservas nunca se muestran todas juntas.
const POR_PAGINA = 8;

interface Filtro {
  estado: EstadoReserva | "";
  sucursal: string;
  recogenHoy: boolean;
  tipoDocumento: TipoDocumento;
  numeroDocumento: string;
}

interface ListaReservasPanelProps {
  sesion: Sesion;
  esPersonal: boolean;
  /** Cambia cuando se crea una reserva nueva: fuerza recargar la lista. */
  version: number;
  onSesionExpirada: () => void;
}

/**
 * Lista de reservas, paginada. El cliente ve solo las suyas; el personal filtra por estado,
 * sucursal de recojo o documento del cliente (arranca viendo su propia sede).
 */
export function ListaReservasPanel({ sesion, esPersonal, version, onSesionExpirada }: ListaReservasPanelProps) {
  // Solo el administrador general elige sucursal: el personal de sucursal ya recibe del servidor únicamente
  // las reservas que salen o llegan a su sede.
  const esGeneral = sesion.rol === "ADMINISTRADOR";

  const [estado, setEstado] = useState<EstadoReserva | "">("");
  const [sucursal, setSucursal] = useState("");
  const [recogenHoy, setRecogenHoy] = useState(false);
  const [tipoDocumento, setTipoDocumento] = useState<TipoDocumento>("DNI");
  const [numeroDocumento, setNumeroDocumento] = useState("");
  const [aplicado, setAplicado] = useState<Filtro>({
    estado: "",
    sucursal: "",
    recogenHoy: false,
    tipoDocumento: "DNI",
    numeroDocumento: "",
  });
  const [pagina, setPagina] = useState(0);

  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [resultado, setResultado] = useState<PaginaReservas | null>(null);

  // Se vuelve a pedir al backend cuando cambia el filtro aplicado, la página o la versión.
  useEffect(() => {
    let activo = true;
    listarReservas(sesion, {
      estado: aplicado.estado || undefined,
      sucursal: aplicado.sucursal || undefined,
      recogenHoy: aplicado.recogenHoy,
      tipoDocumento: aplicado.tipoDocumento,
      numeroDocumento: aplicado.numeroDocumento,
      pagina,
      tamano: POR_PAGINA,
    })
      .then((datos) => {
        if (!activo) return;
        setResultado(datos);
        setError(null);
      })
      .catch((cause: unknown) => {
        if (!activo) return;
        if (cause instanceof SesionExpiradaError) {
          onSesionExpirada();
          return;
        }
        setResultado(null);
        setError(cause instanceof Error ? cause.message : "No se pudieron consultar las reservas.");
      })
      .finally(() => {
        if (activo) setCargando(false);
      });
    return () => {
      activo = false;
    };
  }, [sesion, aplicado, pagina, version, onSesionExpirada]);

  function buscar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setCargando(true);
    setPagina(0);
    setAplicado({ estado, sucursal, recogenHoy, tipoDocumento, numeroDocumento });
  }

  function irAPagina(nueva: number) {
    setCargando(true);
    setPagina(nueva);
  }

  // Al modificar o cancelar una reserva de esta lista, se reemplaza para que se vea el cambio.
  function reemplazar(actualizada: Reserva) {
    setResultado((actual) =>
      actual
        ? {
            ...actual,
            reservas: actual.reservas.map((r) => (r.idReserva === actualizada.idReserva ? actualizada : r)),
          }
        : actual,
    );
  }

  const desde = resultado && resultado.total > 0 ? resultado.pagina * resultado.tamano + 1 : 0;
  const hasta = resultado ? resultado.pagina * resultado.tamano + resultado.reservas.length : 0;

  return (
    <section aria-labelledby="titulo-reservas" className="space-y-5">
      <Card className="rounded-3xl border-0 bg-white shadow-[0_18px_60px_-32px_rgba(24,24,27,0.28)] ring-1 ring-zinc-200/80">
        <CardContent className="p-5 sm:p-7">
          <h2 id="titulo-reservas" className="text-lg font-semibold tracking-tight text-zinc-950">
            {esPersonal ? "Reservas" : "Mis reservas"}
          </h2>
          <p className="mt-1 text-sm text-zinc-500">
            {esPersonal
              ? esGeneral
                ? "Filtra por estado, sucursal de recojo o documento del cliente."
                : "Ves las reservas que salen o llegan a tu sucursal. Filtra por estado o documento del cliente."
              : "Aquí ves tus reservas. Puedes cambiar las fechas o cancelar las que aún están confirmadas."}
          </p>

          <form
            onSubmit={buscar}
            className={`mt-5 grid gap-4 ${esPersonal ? (esGeneral ? "sm:grid-cols-2 lg:grid-cols-[1fr_1.4fr_1.6fr_auto]" : "sm:grid-cols-2 lg:grid-cols-[1fr_1.6fr_auto]") : "sm:grid-cols-[1fr_auto]"} sm:items-end`}
          >
            <div className="space-y-2">
              <label htmlFor="lr-estado" className="text-sm font-semibold text-zinc-800">
                Estado
              </label>
              <select id="lr-estado" value={estado} onChange={(e) => setEstado(e.target.value as EstadoReserva | "")} disabled={cargando} className={CAMPO}>
                <option value="">Todos</option>
                {(Object.keys(ESTADOS_RESERVA) as EstadoReserva[]).map((e) => (
                  <option key={e} value={e}>
                    {ESTADOS_RESERVA[e].etiqueta}
                  </option>
                ))}
              </select>
            </div>

            {esPersonal && (
              <>
                {esGeneral && (
                <div className="space-y-2">
                  <label htmlFor="lr-sucursal" className="text-sm font-semibold text-zinc-800">
                    Sucursal de recojo
                  </label>
                  <select id="lr-sucursal" value={sucursal} onChange={(e) => setSucursal(e.target.value)} disabled={cargando} className={CAMPO}>
                    <option value="">Todas las sucursales</option>
                    {SUCURSALES.map((s) => (
                      <option key={s.value} value={s.value}>
                        {s.label}
                      </option>
                    ))}
                  </select>
                </div>
                )}

                <div className="space-y-2">
                  <label htmlFor="lr-doc" className="text-sm font-semibold text-zinc-800">
                    Documento del cliente
                  </label>
                  <div className="flex gap-2">
                    <select
                      aria-label="Tipo de documento"
                      value={tipoDocumento}
                      onChange={(e) => setTipoDocumento(e.target.value as TipoDocumento)}
                      disabled={cargando}
                      className="h-12 w-28 shrink-0 rounded-xl border border-zinc-200 bg-zinc-50/60 px-3 text-sm text-zinc-900 outline-none transition-colors focus-visible:border-amber-400 focus-visible:ring-3 focus-visible:ring-amber-300/40 disabled:opacity-50"
                    >
                      {TIPOS_DOCUMENTO.map((t) => (
                        <option key={t} value={t}>
                          {t}
                        </option>
                      ))}
                    </select>
                    <Input
                      id="lr-doc"
                      value={numeroDocumento}
                      onChange={(e) => setNumeroDocumento(e.target.value)}
                      maxLength={20}
                      placeholder="Número (opcional)"
                      disabled={cargando}
                      autoComplete="off"
                      className="h-12 rounded-xl border-zinc-200 bg-zinc-50/60 px-4 text-sm"
                    />
                  </div>
                </div>
              </>
            )}

            {esPersonal && (
              <label className="flex items-center gap-2 text-sm font-medium text-zinc-700 sm:col-span-full">
                <input
                  type="checkbox"
                  checked={recogenHoy}
                  onChange={(e) => setRecogenHoy(e.target.checked)}
                  disabled={cargando}
                  className="size-4 rounded border-zinc-300 accent-amber-500"
                />
                Solo las que recogen hoy (clientes que llegan hoy a la sucursal)
              </label>
            )}

            <Button
              type="submit"
              disabled={cargando}
              className="h-12 rounded-xl bg-[#ffd400] px-6 text-sm font-bold text-zinc-950 shadow-[0_7px_18px_-9px_rgba(234,179,8,0.8)] hover:bg-[#f2c900]"
            >
              {cargando ? <LoaderCircle className="size-4 animate-spin" aria-hidden="true" /> : <Search className="size-4" aria-hidden="true" />}
              Buscar
            </Button>
          </form>

          {error && (
            <AutoDismissFeedback key={error} role="alert" className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm leading-5 text-red-700">
              {error}
            </AutoDismissFeedback>
          )}
        </CardContent>
      </Card>

      {resultado && (
        <div className="space-y-4" aria-live="polite">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <p className="text-sm font-medium text-zinc-600">
              {resultado.total === 0
                ? "No hay reservas con esos filtros."
                : `Mostrando ${desde}–${hasta} de ${resultado.total} ${resultado.total === 1 ? "reserva" : "reservas"}`}
            </p>
            {resultado.totalPaginas > 1 && (
              <Paginador pagina={resultado.pagina} totalPaginas={resultado.totalPaginas} deshabilitado={cargando} onCambiar={irAPagina} />
            )}
          </div>

          <div className="grid gap-4 lg:grid-cols-2">
            {resultado.reservas.map((reserva) => (
              <ReservaItem
                key={reserva.idReserva}
                reserva={reserva}
                sesion={sesion}
                esPersonal={esPersonal}
                onActualizada={reemplazar}
                onSesionExpirada={onSesionExpirada}
              />
            ))}
          </div>

          {resultado.totalPaginas > 1 && (
            <div className="flex justify-center">
              <Paginador pagina={resultado.pagina} totalPaginas={resultado.totalPaginas} deshabilitado={cargando} onCambiar={irAPagina} />
            </div>
          )}
        </div>
      )}
    </section>
  );
}

function Paginador({
  pagina,
  totalPaginas,
  deshabilitado,
  onCambiar,
}: {
  pagina: number;
  totalPaginas: number;
  deshabilitado: boolean;
  onCambiar: (pagina: number) => void;
}) {
  return (
    <nav aria-label="Paginación" className="flex items-center gap-2">
      <Button type="button" variant="outline" disabled={deshabilitado || pagina === 0} onClick={() => onCambiar(pagina - 1)} className="h-9 rounded-xl border-zinc-200 px-3">
        <ChevronLeft className="size-4" aria-hidden="true" />
        Anterior
      </Button>
      <span className="px-2 text-sm text-zinc-600">
        Página {pagina + 1} de {totalPaginas}
      </span>
      <Button type="button" variant="outline" disabled={deshabilitado || pagina >= totalPaginas - 1} onClick={() => onCambiar(pagina + 1)} className="h-9 rounded-xl border-zinc-200 px-3">
        Siguiente
        <ChevronRight className="size-4" aria-hidden="true" />
      </Button>
    </nav>
  );
}
