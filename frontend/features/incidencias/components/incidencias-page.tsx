"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AlertTriangle, LoaderCircle } from "lucide-react";
import { CabeceraApp } from "@/components/cabecera-app";
import { cerrarSesion } from "@/features/flota/lib/session";
import { notificarCambioSesion, useSesion } from "@/features/flota/lib/use-session";
import { IncidenciaForm } from "@/features/incidencias/components/incidencia-form";
import { IncidenciasLista } from "@/features/incidencias/components/incidencias-lista";
import type { RegistroIncidencia } from "@/features/incidencias/types/incidencia";

type Pestana = "lista" | "nueva";

function esPersonal(rol: string): boolean {
  return ["AGENTE", "ADMINISTRADOR", "ADMINISTRADOR_SUCURSAL"].includes(rol);
}

export function IncidenciasPage() {
  const router = useRouter();
  const { cargando, sesion } = useSesion();
  const [pestana, setPestana] = useState<Pestana>("lista");
  const [version, setVersion] = useState(0);
  const [registro, setRegistro] = useState<RegistroIncidencia | null>(null);

  useEffect(() => {
    if (!cargando && !sesion) router.replace("/");
  }, [cargando, sesion, router]);

  const salir = useCallback(() => {
    cerrarSesion();
    notificarCambioSesion();
    router.replace("/");
  }, [router]);

  function incidenciaCreada(nuevoRegistro: RegistroIncidencia) {
    setRegistro(nuevoRegistro);
    setVersion((actual) => actual + 1);
    setPestana("lista");
  }

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

  const autorizado = esPersonal(sesion.rol);

  return (
    <div className="min-h-svh bg-[#fafaf9]">
      <CabeceraApp
        nombre={sesion.nombre}
        rol={sesion.rol}
        sucursal={sesion.sucursal}
        actual="/incidencias"
        onSalir={salir}
      />

      <main className="mx-auto w-full max-w-6xl px-5 py-8 sm:px-8 sm:py-10">
        {!autorizado ? (
          <div role="alert" className="rounded-2xl border border-amber-200 bg-amber-50 p-5 text-sm text-amber-900">
            Tu cuenta no tiene acceso al módulo de incidencias.
          </div>
        ) : (
          <>
            <div className="mb-8">
              <p className="mb-2 inline-flex items-center gap-2 text-xs font-semibold tracking-[0.14em] text-amber-700 uppercase">
                <AlertTriangle className="size-4" aria-hidden="true" />
                Control de flota
              </p>
              <h1 className="text-3xl font-semibold tracking-tight text-zinc-950">
                Incidencias
              </h1>
              <p className="mt-2 max-w-2xl text-sm leading-6 text-zinc-500">
                Registra daños, siniestros o necesidades de mantenimiento y consulta el historial de tu sucursal.
              </p>
            </div>

            {registro && (
              <div role="status" className="mb-5 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
                Incidencia registrada para el vehículo {registro.idVehiculo}. Estado: {registro.estado}.
                {" "}El vehículo pasó a {registro.estadoVehiculo}.
              </div>
            )}

            <div role="tablist" aria-label="Incidencias" className="mb-6 flex gap-2 border-b border-zinc-200">
              {([
                { valor: "lista", etiqueta: "Consultar incidencias" },
                { valor: "nueva", etiqueta: "Registrar incidencia" },
              ] as const).map((tab) => (
                <button
                  key={tab.valor}
                  type="button"
                  role="tab"
                  aria-selected={pestana === tab.valor}
                  onClick={() => {
                    setPestana(tab.valor);
                    if (tab.valor === "nueva") setRegistro(null);
                  }}
                  className={`-mb-px border-b-2 px-4 py-3 text-sm font-semibold transition-colors ${
                    pestana === tab.valor
                      ? "border-[#ffd400] text-zinc-950"
                      : "border-transparent text-zinc-500 hover:text-zinc-800"
                  }`}
                >
                  {tab.etiqueta}
                </button>
              ))}
            </div>

            {pestana === "lista" ? (
              <IncidenciasLista
                sesion={sesion}
                version={version}
                onSesionExpirada={salir}
              />
            ) : (
              <IncidenciaForm
                sesion={sesion}
                onCreada={incidenciaCreada}
                onSesionExpirada={salir}
              />
            )}
          </>
        )}
      </main>
    </div>
  );
}
