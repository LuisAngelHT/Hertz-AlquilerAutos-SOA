"use client";

import { useEffect, useState, useSyncExternalStore } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  ArrowUpRight,
  CarFront,
  LogOut,
  Search,
  ShieldCheck,
  UserPlus,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  clearSession,
  getServerSessionSnapshot,
  getSessionSnapshot,
  subscribeToSession,
} from "@/features/auth/lib/auth-session";
import { ClienteBusqueda } from "@/features/clientes/components/cliente-busqueda";
import { ClienteRegistroForm } from "@/features/clientes/components/cliente-registro-form";

type VistaClientes = "buscar" | "registrar";

function esPersonal(rol: string): boolean {
  return rol === "AGENTE" || rol === "ADMINISTRADOR";
}

function nombreRol(rol: string): string {
  switch (rol) {
    case "ADMINISTRADOR":
      return "Administrador";
    case "AGENTE":
      return "Agente";
    case "CLIENTE":
      return "Cliente web";
    default:
      return rol;
  }
}

export function ClientesDashboard() {
  const router = useRouter();
  const sesion = useSyncExternalStore(
    subscribeToSession,
    getSessionSnapshot,
    getServerSessionSnapshot,
  );
  const [vista, setVista] = useState<VistaClientes>("buscar");

  useEffect(() => {
    if (!sesion) {
      router.replace("/");
    }
  }, [router, sesion]);

  function cerrarSesion() {
    clearSession();
    router.replace("/");
  }

  function sesionExpirada() {
    clearSession();
    router.replace("/");
  }

  if (!sesion) {
    return (
      <main className="grid min-h-svh place-items-center bg-[#fafaf9] px-5">
        <div className="text-center">
          <div className="mx-auto grid size-12 place-items-center rounded-2xl bg-[#ffd400] text-zinc-950">
            <CarFront className="size-6" aria-hidden="true" />
          </div>
          <p className="mt-4 text-sm text-zinc-500">Verificando tu sesión...</p>
        </div>
      </main>
    );
  }

  const puedeGestionar = esPersonal(sesion.rol);

  return (
    <main className="min-h-svh bg-[#fafaf9]">
      <header className="bg-[#111110] text-white">
        <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-4 px-5 py-5 sm:px-8 lg:px-10">
          <Link href="/clientes" className="flex items-center gap-3">
            <span className="grid size-10 place-items-center rounded-xl bg-[#ffd400] text-lg font-black text-zinc-950">
              H
            </span>
            <span className="text-lg font-black tracking-[0.16em]">HERTZ</span>
            <span className="hidden border-l border-white/15 pl-3 text-sm text-zinc-400 sm:inline">
              Portal de clientes
            </span>
          </Link>

          <div className="flex items-center gap-3 sm:gap-5">
            <div className="hidden text-right sm:block">
              <p className="text-sm font-medium text-white">{sesion.nombre}</p>
              <p className="mt-0.5 text-xs text-zinc-400">
                {nombreRol(sesion.rol)}
              </p>
            </div>
            <div className="grid size-10 place-items-center rounded-full bg-white/10 text-sm font-semibold text-[#ffd400]">
              {sesion.nombre.trim().charAt(0).toUpperCase()}
            </div>
            <Button
              type="button"
              variant="ghost"
              onClick={cerrarSesion}
              className="h-10 rounded-xl px-3 text-zinc-300 hover:bg-white/10 hover:text-white"
            >
              <LogOut className="size-4" aria-hidden="true" />
              <span className="hidden sm:inline">Salir</span>
            </Button>
          </div>
        </div>
      </header>

      {puedeGestionar && (
        <section className="mx-auto max-w-7xl px-5 py-8 sm:px-8 sm:py-10 lg:px-10">
          <div className="mb-8 flex flex-col justify-between gap-5 md:flex-row md:items-end">
            <div>
              <p className="mb-2 inline-flex items-center gap-2 text-xs font-semibold tracking-[0.14em] text-amber-700 uppercase">
                <ShieldCheck className="size-4" aria-hidden="true" />
                Gestión segura
              </p>
              <h1 className="text-3xl font-semibold tracking-tight text-zinc-950 sm:text-4xl">
                Gestión de clientes
              </h1>
              <p className="mt-2 max-w-2xl text-sm leading-6 text-zinc-500 sm:text-base">
                Busca fichas por documento, revisa la información y mantén los
                datos actualizados.
              </p>
            </div>
            <div className="inline-flex w-fit items-center gap-2 rounded-full border border-zinc-200 bg-white px-3.5 py-2 text-xs font-medium text-zinc-500">
              <span className="size-2 rounded-full bg-emerald-500" />
              Sesión activa
              <ArrowUpRight className="size-3.5" aria-hidden="true" />
            </div>
          </div>

          <div className="mb-6 flex w-full flex-wrap gap-2 border-b border-zinc-200 pb-3">
            <Button
              type="button"
              variant={vista === "buscar" ? "default" : "ghost"}
              onClick={() => setVista("buscar")}
              className={
                vista === "buscar"
                  ? "h-10 rounded-xl bg-zinc-950 px-4 text-white hover:bg-zinc-800"
                  : "h-10 rounded-xl px-4 text-zinc-600"
              }
            >
              <Search className="size-4" aria-hidden="true" />
              Buscar cliente
            </Button>
            <Button
              type="button"
              variant={vista === "registrar" ? "default" : "ghost"}
              onClick={() => setVista("registrar")}
              className={
                vista === "registrar"
                  ? "h-10 rounded-xl bg-zinc-950 px-4 text-white hover:bg-zinc-800"
                  : "h-10 rounded-xl px-4 text-zinc-600"
              }
            >
              <UserPlus className="size-4" aria-hidden="true" />
              Registrar cliente
            </Button>
          </div>

          <div className="mx-auto max-w-5xl">
            {vista === "buscar" ? (
              <ClienteBusqueda
                token={sesion.token}
                onAuthExpired={sesionExpirada}
              />
            ) : (
              <ClienteRegistroForm
                token={sesion.token}
                modalidad="presencial"
                onAuthExpired={sesionExpirada}
              />
            )}
          </div>
        </section>
      )}
    </main>
  );
}
