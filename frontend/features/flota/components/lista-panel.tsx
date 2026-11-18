"use client";

import { useEffect, useState, type FormEvent } from "react";
import { ChevronLeft, ChevronRight, LoaderCircle, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { VehiculoCard } from "@/features/flota/components/vehiculo-card";
import { SUCURSALES } from "@/features/flota/lib/constants";
import {
  listarVehiculos,
  SesionExpiradaError,
  type EstadoVehiculo,
  type PaginaVehiculos,
  type Vehiculo,
} from "@/features/flota/lib/flota-api";
import type { Sesion } from "@/features/flota/lib/session";

const CAMPO =
  "h-12 w-full rounded-xl border border-zinc-200 bg-zinc-50/60 px-4 text-sm text-zinc-900 outline-none transition-colors focus-visible:border-amber-400 focus-visible:ring-3 focus-visible:ring-amber-300/40 disabled:opacity-50";

const ESTADOS: { value: EstadoVehiculo; label: string }[] = [
  { value: "DISPONIBLE", label: "Disponibles" },
  { value: "ALQUILADO", label: "Alquilados" },
  { value: "MANTENIMIENTO", label: "En mantenimiento" },
  { value: "FUERA_DE_SERVICIO", label: "Fuera de servicio (bajas)" },
];

// Vehículos por página: con cientos de vehículos nunca se muestran todos juntos.
const POR_PAGINA = 12;

interface Filtro {
  texto: string;
  sucursal: string; // vacío = todas
  estado: EstadoVehiculo;
}

interface ListaPanelProps {
  sesion: Sesion;
  puedeEditar: boolean;
  onSesionExpirada: () => void;
}

/**
 * Lista de vehículos por sucursal y por marca o modelo, paginada. Se carga sola al abrir la pantalla.
 * - Cliente (solo consulta): busca vehículos disponibles y ve en qué sede está cada uno.
 * - Personal (AGENTE / ADMINISTRADOR / ADMINISTRADOR_SUCURSAL): además ve otros estados y los cambia.
 */
export function ListaPanel({ sesion, puedeEditar, onSesionExpirada }: ListaPanelProps) {
  // El personal de sucursal arranca viendo su sede; el administrador general y el cliente, todas.
  const sucursalInicial =
    puedeEditar && sesion.rol !== "ADMINISTRADOR" ? (sesion.sucursal ?? "") : "";

  // Lo que se está escribiendo en el formulario (todavía sin buscar).
  const [texto, setTexto] = useState("");
  const [sucursal, setSucursal] = useState(sucursalInicial);
  const [estado, setEstado] = useState<EstadoVehiculo>("DISPONIBLE");
  // Lo que ya se buscó (filtro aplicado) y la página que se está viendo.
  const [aplicado, setAplicado] = useState<Filtro>({
    texto: "",
    sucursal: sucursalInicial,
    estado: "DISPONIBLE",
  });
  const [pagina, setPagina] = useState(0);

  const [buscando, setBuscando] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [resultado, setResultado] = useState<PaginaVehiculos | null>(null);

  // Se vuelve a pedir al backend cada vez que cambia el filtro aplicado o la página.
  useEffect(() => {
    let activo = true;
    // La sucursal del usuario sale primero; luego las demás por orden alfabético.
    listarVehiculos(sesion, {
      ...aplicado,
      sucursalPrioritaria: sesion.sucursal ?? undefined,
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
        setError(cause instanceof Error ? cause.message : "No se pudo consultar la flota.");
      })
      .finally(() => {
        if (activo) setBuscando(false);
      });
    return () => {
      activo = false;
    };
  }, [sesion, aplicado, pagina, onSesionExpirada]);

  function buscar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBuscando(true);
    setPagina(0); // una búsqueda nueva siempre empieza en la primera página
    setAplicado({ texto, sucursal, estado });
  }

  function irAPagina(nueva: number) {
    setBuscando(true);
    setPagina(nueva);
  }

  // Al cambiar de estado un vehículo de esta lista, se reemplaza para que se vea el cambio.
  function reemplazar(actualizado: Vehiculo) {
    setResultado((actual) =>
      actual
        ? {
            ...actual,
            vehiculos: actual.vehiculos.map((v) => (v.placa === actualizado.placa ? actualizado : v)),
          }
        : actual,
    );
  }

  const desde = resultado && resultado.total > 0 ? resultado.pagina * resultado.tamano + 1 : 0;
  const hasta = resultado ? resultado.pagina * resultado.tamano + resultado.vehiculos.length : 0;

  return (
    <section aria-labelledby="titulo-lista" className="space-y-5">
      <Card className="rounded-3xl border-0 bg-white shadow-[0_18px_60px_-32px_rgba(24,24,27,0.28)] ring-1 ring-zinc-200/80">
        <CardContent className="p-5 sm:p-7">
          <h2 id="titulo-lista" className="text-lg font-semibold tracking-tight text-zinc-950">
            {puedeEditar ? "Vehículos de la flota" : "Buscar un vehículo"}
          </h2>
          <p className="mt-1 text-sm text-zinc-500">
            {puedeEditar
              ? "Filtra por sucursal, estado o por marca y modelo."
              : "Busca por marca o modelo y mira en qué sucursal está."}
          </p>

          <form
            onSubmit={buscar}
            className={`mt-5 grid gap-4 ${puedeEditar ? "sm:grid-cols-[2fr_1.5fr_1fr_auto]" : "sm:grid-cols-[2fr_1.5fr_auto]"} sm:items-end`}
          >
            <div className="space-y-2">
              <label htmlFor="texto" className="text-sm font-semibold text-zinc-800">
                Marca o modelo
              </label>
              <Input
                id="texto"
                value={texto}
                onChange={(e) => setTexto(e.target.value)}
                placeholder="Toyota, RAV4, Hilux..."
                maxLength={60}
                disabled={buscando}
                autoComplete="off"
                className="h-12 rounded-xl border-zinc-200 bg-zinc-50/60 px-4 text-sm focus-visible:border-amber-400 focus-visible:ring-amber-300/40"
              />
            </div>

            <div className="space-y-2">
              <label htmlFor="lista-sucursal" className="text-sm font-semibold text-zinc-800">
                Sucursal
              </label>
              <select
                id="lista-sucursal"
                value={sucursal}
                onChange={(e) => setSucursal(e.target.value)}
                disabled={buscando}
                className={CAMPO}
              >
                <option value="">Todas las sucursales</option>
                {SUCURSALES.map((s) => (
                  <option key={s.value} value={s.value}>
                    {s.label}
                  </option>
                ))}
              </select>
            </div>

            {puedeEditar && (
              <div className="space-y-2">
                <label htmlFor="lista-estado" className="text-sm font-semibold text-zinc-800">
                  Estado
                </label>
                <select
                  id="lista-estado"
                  value={estado}
                  onChange={(e) => setEstado(e.target.value as EstadoVehiculo)}
                  disabled={buscando}
                  className={CAMPO}
                >
                  {ESTADOS.filter((e) => e.value !== "FUERA_DE_SERVICIO" || sesion.rol === "ADMINISTRADOR").map((e) => (
                    <option key={e.value} value={e.value}>
                      {e.label}
                    </option>
                  ))}
                </select>
              </div>
            )}

            <Button
              type="submit"
              disabled={buscando}
              className="h-12 rounded-xl bg-[#ffd400] px-6 text-sm font-bold text-zinc-950 shadow-[0_7px_18px_-9px_rgba(234,179,8,0.8)] hover:bg-[#f2c900]"
            >
              {buscando ? (
                <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
              ) : (
                <Search className="size-4" aria-hidden="true" />
              )}
              Buscar
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
          <div className="flex flex-wrap items-center justify-between gap-3">
            <p className="text-sm font-medium text-zinc-600">
              {resultado.total === 0
                ? "No se encontraron vehículos con esos filtros."
                : `Mostrando ${desde}–${hasta} de ${resultado.total} ${resultado.total === 1 ? "vehículo" : "vehículos"}`}
            </p>
            {resultado.totalPaginas > 1 && (
              <Paginador
                pagina={resultado.pagina}
                totalPaginas={resultado.totalPaginas}
                deshabilitado={buscando}
                onCambiar={irAPagina}
              />
            )}
          </div>

          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {resultado.vehiculos.map((vehiculo) => (
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

          {resultado.totalPaginas > 1 && (
            <div className="flex justify-center">
              <Paginador
                pagina={resultado.pagina}
                totalPaginas={resultado.totalPaginas}
                deshabilitado={buscando}
                onCambiar={irAPagina}
              />
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
      <Button
        type="button"
        variant="outline"
        disabled={deshabilitado || pagina === 0}
        onClick={() => onCambiar(pagina - 1)}
        className="h-9 rounded-xl border-zinc-200 px-3"
      >
        <ChevronLeft className="size-4" aria-hidden="true" />
        Anterior
      </Button>
      <span className="px-2 text-sm text-zinc-600">
        Página {pagina + 1} de {totalPaginas}
      </span>
      <Button
        type="button"
        variant="outline"
        disabled={deshabilitado || pagina >= totalPaginas - 1}
        onClick={() => onCambiar(pagina + 1)}
        className="h-9 rounded-xl border-zinc-200 px-3"
      >
        Siguiente
        <ChevronRight className="size-4" aria-hidden="true" />
      </Button>
    </nav>
  );
}
