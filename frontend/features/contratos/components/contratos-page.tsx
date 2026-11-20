"use client";

import { useCallback, useEffect } from "react";
import { useRouter } from "next/navigation";
import { FileSignature, LoaderCircle } from "lucide-react";
import { CabeceraApp } from "@/components/cabecera-app";
import { cerrarSesion } from "@/features/flota/lib/session";
import { notificarCambioSesion, useSesion } from "@/features/flota/lib/use-session";
import { ContratosLista } from "@/features/contratos/components/contratos-lista";

const ROLES_PERSONAL = [
  "AGENTE",
  "ADMINISTRADOR",
  "ADMINISTRADOR_SUCURSAL",
];

export function ContratosPage() {
  const router = useRouter();
  const { cargando, sesion } = useSesion();

  useEffect(() => {
    if (!cargando && !sesion) router.replace("/");
  }, [cargando, sesion, router]);

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

  const autorizado = ROLES_PERSONAL.includes(sesion.rol);

  return (
    <div className="min-h-svh bg-[#fafaf9]">
      <CabeceraApp
        nombre={sesion.nombre}
        rol={sesion.rol}
        sucursal={sesion.sucursal}
        actual="/contratos"
        onSalir={salir}
      />

      <main className="mx-auto w-full max-w-6xl px-5 py-8 sm:px-8 sm:py-10">
        {!autorizado ? (
          <div
            role="alert"
            className="rounded-2xl border border-amber-200 bg-amber-50 p-5 text-sm text-amber-900"
          >
            Tu cuenta no tiene acceso al módulo de contratos.
          </div>
        ) : (
          <>
            <div className="mb-8">
              <p className="mb-2 inline-flex items-center gap-2 text-xs font-semibold tracking-[0.14em] text-amber-700 uppercase">
                <FileSignature className="size-4" aria-hidden="true" />
                Alquiler de vehículos
              </p>
              <h1 className="text-3xl font-semibold tracking-tight text-zinc-950">
                Contratos
              </h1>
              <p className="mt-2 max-w-2xl text-sm leading-6 text-zinc-500">
                Consulta los contratos de alquiler. Cada sucursal ve los de su sede y el administrador
                general ve todos. Se generan al registrar la entrega del vehículo en Reservas y se
                finalizan al completar su devolución.
              </p>
            </div>

            <ContratosLista sesion={sesion} onSesionExpirada={salir} />
          </>
        )}
      </main>
    </div>
  );
}
