"use client";

import { useEffect, useState, type FormEvent } from "react";
import { BadgeCheck, ChevronLeft, ChevronRight, LoaderCircle, Search } from "lucide-react";
import { AutoDismissFeedback } from "@/components/auto-dismiss-feedback";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { ApiRequestError } from "@/lib/api-client";
import { listarClientes, type PaginaClientes } from "@/features/clientes/lib/cliente-api";

// Clientes por página: con muchos clientes nunca se muestran todos juntos.
const POR_PAGINA = 10;

interface ClientesListaProps {
  token: string;
  onAuthExpired: () => void;
}

/** Todos los clientes registrados, paginados y con búsqueda por nombre, documento o correo. */
export function ClientesLista({ token, onAuthExpired }: ClientesListaProps) {
  const [texto, setTexto] = useState("");
  const [aplicado, setAplicado] = useState("");
  const [pagina, setPagina] = useState(0);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [resultado, setResultado] = useState<PaginaClientes | null>(null);

  useEffect(() => {
    let activo = true;
    listarClientes(token, { texto: aplicado, pagina, tamano: POR_PAGINA })
      .then((datos) => {
        if (!activo) return;
        setResultado(datos);
        setError(null);
      })
      .catch((cause: unknown) => {
        if (!activo) return;
        if (cause instanceof ApiRequestError && cause.status === 401) {
          onAuthExpired();
          return;
        }
        setResultado(null);
        setError(cause instanceof Error ? cause.message : "No se pudo consultar a los clientes.");
      })
      .finally(() => {
        if (activo) setCargando(false);
      });
    return () => {
      activo = false;
    };
  }, [token, aplicado, pagina, onAuthExpired]);

  function buscar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setCargando(true);
    setPagina(0);
    setAplicado(texto);
  }

  function irAPagina(nueva: number) {
    setCargando(true);
    setPagina(nueva);
  }

  const desde = resultado && resultado.total > 0 ? resultado.pagina * resultado.tamano + 1 : 0;
  const hasta = resultado ? resultado.pagina * resultado.tamano + resultado.clientes.length : 0;

  return (
    <section aria-labelledby="titulo-todos" className="space-y-5">
      <Card className="rounded-2xl border-0 bg-white shadow-sm ring-1 ring-zinc-200/80">
        <CardContent className="p-5 sm:p-7">
          <h2 id="titulo-todos" className="text-lg font-semibold text-zinc-950">
            Todos los clientes
          </h2>
          <p className="mt-1 text-sm text-zinc-500">
            Busca por nombre, documento o correo, o recorre la lista completa.
          </p>
          <form onSubmit={buscar} className="mt-5 flex flex-col gap-3 sm:flex-row">
            <Input
              value={texto}
              onChange={(e) => setTexto(e.target.value)}
              placeholder="Nombre, documento o correo"
              aria-label="Buscar clientes"
              maxLength={60}
              autoComplete="off"
              className="h-11 rounded-xl border-zinc-200 bg-white px-3 text-sm"
            />
            <Button
              type="submit"
              disabled={cargando}
              className="h-11 rounded-xl bg-[#ffd400] px-5 font-semibold text-zinc-950 hover:bg-[#f2c900]"
            >
              {cargando ? <LoaderCircle className="size-4 animate-spin" aria-hidden="true" /> : <Search className="size-4" aria-hidden="true" />}
              Buscar
            </Button>
          </form>
          {error && (
            <AutoDismissFeedback key={error} role="alert" className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
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
                ? "No hay clientes con esa búsqueda."
                : `Mostrando ${desde}–${hasta} de ${resultado.total} ${resultado.total === 1 ? "cliente" : "clientes"}`}
            </p>
            {resultado.totalPaginas > 1 && (
              <Paginador pagina={resultado.pagina} totalPaginas={resultado.totalPaginas} deshabilitado={cargando} onCambiar={irAPagina} />
            )}
          </div>

          <div className="overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-zinc-200/80">
            <ul className="divide-y divide-zinc-100">
              {resultado.clientes.map((c) => (
                <li key={c.idCliente} className="grid gap-1 px-5 py-4 sm:grid-cols-[2fr_1.2fr_1.6fr_auto] sm:items-center sm:gap-4">
                  <div>
                    <p className="text-sm font-semibold text-zinc-900">
                      {c.apellidos}, {c.nombres}
                    </p>
                    <p className="text-xs text-zinc-500">
                      {c.tipoDocumento} · {c.numeroDocumento}
                    </p>
                  </div>
                  <p className="text-sm text-zinc-600">{c.telefono ?? "Sin teléfono"}</p>
                  <p className="break-all text-sm text-zinc-600">{c.email}</p>
                  <span
                    className={`inline-flex w-fit items-center gap-1.5 rounded-full px-3 py-1 text-xs font-semibold ${
                      c.licenciaVigente ? "bg-emerald-50 text-emerald-700" : "bg-zinc-100 text-zinc-600"
                    }`}
                  >
                    <BadgeCheck className="size-3.5" aria-hidden="true" />
                    {c.licenciaVigente ? "Licencia vigente" : "Sin licencia vigente"}
                  </span>
                </li>
              ))}
            </ul>
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
