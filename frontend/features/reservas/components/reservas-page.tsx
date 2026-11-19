"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams, type ReadonlyURLSearchParams } from "next/navigation";
import { LoaderCircle } from "lucide-react";
import { CabeceraApp } from "@/components/cabecera-app";
import { cerrarSesion, puedeCambiarEstado } from "@/features/flota/lib/session";
import { notificarCambioSesion, useSesion } from "@/features/flota/lib/use-session";
import { ListaReservasPanel } from "@/features/reservas/components/lista-reservas-panel";
import { NuevaReservaPanel, type PrefillReserva } from "@/features/reservas/components/nueva-reserva-panel";

/**
 * Pantalla de reservas (SVC-02). Solo se ve con sesión iniciada: si no hay una sesión válida
 * del login, manda de vuelta a la pantalla de inicio de sesión ("/").
 *  - CLIENTE: reserva para sí mismo y ve / cambia / cancela sus reservas.
 *  - Personal (AGENTE / ADMINISTRADOR / ADMINISTRADOR_SUCURSAL): reserva a nombre de un cliente,
 *    gestiona las reservas de su sucursal (el administrador general, todas), entrega y recibe vehículos.
 */
type Pestana = "reservas" | "nueva";

export function ReservasPage() {
  // Desde el catálogo del cliente se llega con ?nueva=1&categoria=..&sucursal=..&desde=..&hasta=..
  const params = useSearchParams();
  // Con "key", si la dirección cambia (o llega recién después de montar la página) la pantalla se
  // reconstruye y abre en "Nueva reserva" con los datos del catálogo, en vez de quedarse en "Mis reservas".
  return <ReservasContenido key={params.toString()} params={params} />;
}

function ReservasContenido({ params }: { params: ReadonlyURLSearchParams }) {
  const router = useRouter();
  const prefill = useMemo<PrefillReserva>(
    () => ({
      categoria: params.get("categoria") ?? undefined,
      sucursal: params.get("sucursal") ?? undefined,
      desde: params.get("desde") ?? undefined,
      hasta: params.get("hasta") ?? undefined,
      placa: params.get("placa") ?? undefined,
      vehiculo: params.get("vehiculo") ?? undefined,
    }),
    [params],
  );
  const [pestana, setPestana] = useState<Pestana>(params.get("nueva") === "1" ? "nueva" : "reservas");
  // Sube cada vez que se crea una reserva, para que la lista se recargue.
  const [version, setVersion] = useState(0);
  const { cargando, sesion } = useSesion();

  useEffect(() => {
    if (!cargando && !sesion) {
      router.replace("/");
    }
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

  const esPersonal = puedeCambiarEstado(sesion.rol);
  const PESTANAS: { valor: Pestana; etiqueta: string }[] = [
    { valor: "reservas", etiqueta: esPersonal ? "Reservas" : "Mis reservas" },
    { valor: "nueva", etiqueta: "Nueva reserva" },
  ];

  return (
    <div className="min-h-svh bg-[#fafaf9]">
      <CabeceraApp
        nombre={sesion.nombre}
        rol={sesion.rol}
        sucursal={sesion.sucursal}
        actual="/reservas"
        onSalir={salir}
      />

      <main className="mx-auto w-full max-w-6xl px-5 py-8 sm:px-8 sm:py-10">
        <div className="mb-8">
          <h1 className="text-3xl font-semibold tracking-tight text-zinc-950">{esPersonal ? "Reservas" : "Mis reservas"}</h1>
          <p className="mt-2 max-w-2xl text-sm leading-6 text-zinc-500">
            {esPersonal
              ? "Crea reservas a nombre de un cliente, cámbialas o cancélalas, y registra la entrega y la devolución del vehículo."
              : "Reserva tu vehículo y administra tus reservas."}
          </p>
        </div>

        <div role="tablist" aria-label="Reservas" className="mb-6 flex flex-wrap gap-2 border-b border-zinc-200">
          {PESTANAS.map((p) => (
            <button
              key={p.valor}
              type="button"
              role="tab"
              id={`tab-${p.valor}`}
              aria-selected={pestana === p.valor}
              aria-controls={`panel-${p.valor}`}
              onClick={() => setPestana(p.valor)}
              className={`-mb-px border-b-2 px-4 py-3 text-sm font-semibold transition-colors ${
                pestana === p.valor ? "border-[#ffd400] text-zinc-950" : "border-transparent text-zinc-500 hover:text-zinc-800"
              }`}
            >
              {p.etiqueta}
            </button>
          ))}
        </div>

        <div role="tabpanel" id="panel-reservas" aria-labelledby="tab-reservas" hidden={pestana !== "reservas"}>
          <ListaReservasPanel sesion={sesion} esPersonal={esPersonal} version={version} onSesionExpirada={salir} />
        </div>
        <div role="tabpanel" id="panel-nueva" aria-labelledby="tab-nueva" hidden={pestana !== "nueva"}>
          <NuevaReservaPanel
            key={params.toString()}
            prefill={prefill}
            sesion={sesion}
            esPersonal={esPersonal}
            onCreada={() => setVersion((v) => v + 1)}
            onSesionExpirada={salir}
          />
        </div>
      </main>
    </div>
  );
}
