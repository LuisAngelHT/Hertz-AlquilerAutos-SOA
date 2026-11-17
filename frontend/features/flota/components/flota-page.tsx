"use client";

import { useCallback, useEffect } from "react";
import { useRouter } from "next/navigation";
import { Info, LoaderCircle, LogOut } from "lucide-react";
import { Button } from "@/components/ui/button";
import { BuscarPlacaPanel } from "@/features/flota/components/buscar-placa-panel";
import { DisponibilidadPanel } from "@/features/flota/components/disponibilidad-panel";
import { cerrarSesion, puedeCambiarEstado } from "@/features/flota/lib/session";
import { notificarCambioSesion, useSesion } from "@/features/flota/lib/use-session";

/**
 * Pantalla de gestión de flota (SVC-03). Solo se ve con sesión iniciada: si no hay una
 * sesión válida del login, manda de vuelta a la pantalla de inicio de sesión ("/").
 */
export function FlotaPage() {
  const router = useRouter();
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

  return (
    <div className="min-h-svh bg-[#fafaf9]">
      <header className="bg-[#111110] text-white">
        <div className="mx-auto flex w-full max-w-6xl items-center justify-between gap-4 px-5 py-4 sm:px-8">
          <div className="flex items-center gap-3" aria-label="Hertz">
            <span className="grid size-10 place-items-center rounded-xl bg-[#ffd400] text-lg font-black tracking-tighter text-zinc-950">
              H
            </span>
            <span className="text-lg font-black tracking-[0.16em]">HERTZ</span>
            <span className="ml-2 hidden rounded-full border border-white/15 px-3 py-1 text-xs font-medium tracking-wide text-zinc-300 sm:inline">
              FLOTA
            </span>
          </div>

          <div className="flex items-center gap-4">
            <div className="hidden text-right sm:block">
              <p className="text-sm font-semibold">{sesion.nombre}</p>
              <p className="text-xs text-zinc-400">{sesion.rol}</p>
            </div>
            <Button
              type="button"
              variant="outline"
              onClick={salir}
              className="h-10 rounded-xl border-white/20 bg-transparent px-4 text-white hover:bg-white/10 hover:text-white"
            >
              <LogOut className="size-4" aria-hidden="true" />
              Cerrar sesión
            </Button>
          </div>
        </div>
      </header>

      <main className="mx-auto w-full max-w-6xl px-5 py-8 sm:px-8 sm:py-10">
        <div className="mb-8">
          <h1 className="text-3xl font-semibold tracking-tight text-zinc-950">
            Gestión de flota
          </h1>
          <p className="mt-2 max-w-2xl text-sm leading-6 text-zinc-500">
            Consulta qué vehículos están disponibles por sucursal, categoría y fechas,
            y registra los cambios de estado en el mostrador.
          </p>
        </div>

        {!puedeEditar && (
          <div
            role="status"
            className="mb-6 flex items-start gap-3 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm leading-5 text-amber-900"
          >
            <Info className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
            <p>
              Tu rol ({sesion.rol}) es de solo consulta. Cambiar el estado de un vehículo
              lo pueden hacer los roles AGENTE y ADMINISTRADOR.
            </p>
          </div>
        )}

        <div className="grid gap-8 lg:grid-cols-[1.6fr_1fr] lg:items-start">
          <DisponibilidadPanel
            sesion={sesion}
            puedeEditar={puedeEditar}
            onSesionExpirada={salir}
          />
          <BuscarPlacaPanel
            sesion={sesion}
            puedeEditar={puedeEditar}
            onSesionExpirada={salir}
          />
        </div>
      </main>
    </div>
  );
}
