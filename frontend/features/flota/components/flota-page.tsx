"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Info, LoaderCircle } from "lucide-react";
import { CabeceraApp } from "@/components/cabecera-app";
import { AgregarVehiculoPanel } from "@/features/flota/components/agregar-vehiculo-panel";
import { BuscarPlacaPanel } from "@/features/flota/components/buscar-placa-panel";
import { DisponibilidadPanel } from "@/features/flota/components/disponibilidad-panel";
import { ListaPanel } from "@/features/flota/components/lista-panel";
import {
  cerrarSesion,
  puedeCambiarEstado,
  requiereSucursal,
} from "@/features/flota/lib/session";
import { notificarCambioSesion, useSesion } from "@/features/flota/lib/use-session";

/**
 * Pantalla de gestión de flota (SVC-03). Solo se ve con sesión iniciada: si no hay una
 * sesión válida del login, manda de vuelta a la pantalla de inicio de sesión ("/").
 */
type Pestana = "vehiculos" | "fechas" | "placa" | "agregar";

const PESTANAS: { valor: Pestana; etiqueta: string }[] = [
  { valor: "vehiculos", etiqueta: "Vehículos" },
  { valor: "fechas", etiqueta: "Disponibilidad por fechas" },
  { valor: "placa", etiqueta: "Buscar por placa" },
  { valor: "agregar", etiqueta: "Agregar vehículo" },
];

export function FlotaPage() {
  const router = useRouter();
  // Pestaña activa. Los tres paneles quedan montados (solo se ocultan) para no perder la búsqueda.
  const [pestana, setPestana] = useState<Pestana>("vehiculos");
  const { cargando, sesion } = useSesion();

  useEffect(() => {
    if (!cargando && !sesion) {
      router.replace("/");
    }
  }, [cargando, sesion, router]);

  // Se usa al cerrar sesión y cuando el backend responde 401 (token vencido).
  const salir = useCallback(() => {
    cerrarSesion();
    notificarCambioSesion();
    router.replace("/");
  }, [router]);

  if (cargando || !sesion) {
    return (
      <main className="grid min-h-svh place-items-center bg-[#fafaf9]">
        <p className="inline-flex items-center gap-2 text-sm text-zinc-500">
          <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
          Verificando sesión...
        </p>
      </main>
    );
  }

  const puedeEditar = puedeCambiarEstado(sesion.rol);
  // AGENTE y ADMINISTRADOR_SUCURSAL necesitan una sucursal asignada para modificar vehículos.
  const faltaSucursal = requiereSucursal(sesion.rol) && sesion.sucursal === null;

  return (
    <div className="min-h-svh bg-[#fafaf9]">
      <CabeceraApp
        nombre={sesion.nombre}
        rol={sesion.rol}
        sucursal={sesion.sucursal}
        actual="/flota"
        onSalir={salir}
      />

      <main className="mx-auto w-full max-w-6xl px-5 py-8 sm:px-8 sm:py-10">
        <div className="mb-8">
          <h1 className="text-3xl font-semibold tracking-tight text-zinc-950">
            {puedeEditar ? "Gestión de flota" : "Vehículos"}
          </h1>
          <p className="mt-2 max-w-2xl text-sm leading-6 text-zinc-500">
            {puedeEditar
              ? "Consulta qué vehículos están disponibles por sucursal, categoría y fechas, y registra los cambios de estado en el mostrador."
              : "Mira los vehículos de nuestra flota, en qué sucursal están y cuáles están libres en tus fechas. Para reservar, ve a Inicio o a Mis reservas."}
          </p>
        </div>

        {faltaSucursal && (
          <div
            role="status"
            className="mb-6 flex items-start gap-3 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm leading-5 text-amber-900"
          >
            <Info className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
            <p>
              Tu usuario no tiene una sucursal asignada, así que por ahora solo puedes consultar.
              Pídele al administrador que te asigne tu sucursal.
            </p>
          </div>
        )}

        {/* Pestañas: cada herramienta ocupa todo el ancho y siempre está a un clic, sin importar
            cuántos vehículos haya en la lista. El cliente no ve "Buscar por placa" (es del personal). */}
        <div
          role="tablist"
          aria-label="Herramientas de flota"
          className="mb-6 flex flex-wrap gap-2 border-b border-zinc-200"
        >
          {PESTANAS.filter(
            (p) =>
              (p.valor !== "placa" || puedeEditar) && (p.valor !== "agregar" || sesion.rol === "ADMINISTRADOR"),
          ).map((p) => (
            <button
              key={p.valor}
              type="button"
              role="tab"
              id={`tab-${p.valor}`}
              aria-selected={pestana === p.valor}
              aria-controls={`panel-${p.valor}`}
              onClick={() => setPestana(p.valor)}
              className={`-mb-px border-b-2 px-4 py-3 text-sm font-semibold transition-colors ${
                pestana === p.valor
                  ? "border-[#ffd400] text-zinc-950"
                  : "border-transparent text-zinc-500 hover:text-zinc-800"
              }`}
            >
              {p.etiqueta}
            </button>
          ))}
        </div>

        <div
          role="tabpanel"
          id="panel-vehiculos"
          aria-labelledby="tab-vehiculos"
          hidden={pestana !== "vehiculos"}
        >
          <ListaPanel sesion={sesion} puedeEditar={puedeEditar} onSesionExpirada={salir} />
        </div>
        <div
          role="tabpanel"
          id="panel-fechas"
          aria-labelledby="tab-fechas"
          hidden={pestana !== "fechas"}
        >
          <DisponibilidadPanel
            sesion={sesion}
            puedeEditar={puedeEditar}
            onSesionExpirada={salir}
          />
        </div>
        {sesion.rol === "ADMINISTRADOR" && (
          <div
            role="tabpanel"
            id="panel-agregar"
            aria-labelledby="tab-agregar"
            hidden={pestana !== "agregar"}
          >
            <AgregarVehiculoPanel sesion={sesion} onSesionExpirada={salir} />
          </div>
        )}
        {puedeEditar && (
          <div
            role="tabpanel"
            id="panel-placa"
            aria-labelledby="tab-placa"
            hidden={pestana !== "placa"}
            className="max-w-xl"
          >
            <BuscarPlacaPanel
              sesion={sesion}
              puedeEditar={puedeEditar}
              onSesionExpirada={salir}
            />
          </div>
        )}
      </main>
    </div>
  );
}
