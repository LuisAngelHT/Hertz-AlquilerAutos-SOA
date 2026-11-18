"use client";

import Link from "next/link";
import { House, LogOut } from "lucide-react";
import { Button } from "@/components/ui/button";
import { etiquetaSucursal } from "@/features/flota/lib/constants";

interface CabeceraAppProps {
  nombre: string;
  rol: string;
  sucursal?: string | null;
  /** Ruta de la pantalla actual ("/clientes", "/reservas" o "/flota"): se resalta en el menú. */
  actual: "/clientes" | "/reservas" | "/flota";
  onSalir: () => void;
}

interface Enlace {
  ruta: "/clientes" | "/reservas" | "/flota";
  etiqueta: string;
}

/**
 * Menú según quién entró. El cliente ve nombres pensados para él (Inicio, Vehículos, Mis reservas);
 * el personal ve sus secciones de trabajo (Clientes, Reservas, Flota). Todo el mundo ve "Hola, nombre"
 * (un saludo sin género: "Bienvenido" no le sirve a todas las personas).
 */
function enlacesDe(rol: string): Enlace[] {
  if (rol === "CLIENTE") {
    return [
      { ruta: "/clientes", etiqueta: "Inicio" },
      { ruta: "/flota", etiqueta: "Vehículos" },
      { ruta: "/reservas", etiqueta: "Mis reservas" },
    ];
  }
  return [
    { ruta: "/clientes", etiqueta: "Clientes" },
    { ruta: "/reservas", etiqueta: "Reservas" },
    { ruta: "/flota", etiqueta: "Flota" },
  ];
}

function nombreDelRol(rol: string): string {
  switch (rol) {
    case "ADMINISTRADOR":
      return "Administrador";
    case "ADMINISTRADOR_SUCURSAL":
      return "Administrador de sucursal";
    case "AGENTE":
      return "Agente";
    case "CLIENTE":
      return "Cliente";
    default:
      return rol;
  }
}

// "Ana Prueba (cliente)" -> "Ana Prueba": se quita la aclaración entre paréntesis del nombre.
function nombreLimpio(nombre: string): string {
  return nombre.replace(/\s*\(.*?\)\s*/g, " ").trim() || nombre;
}

export function CabeceraApp({ nombre, rol, sucursal, actual, onSalir }: CabeceraAppProps) {
  const enlaces = enlacesDe(rol);
  const esCliente = rol === "CLIENTE";

  return (
    <header className="bg-[#111110] text-white">
      <div className="mx-auto flex w-full max-w-7xl flex-wrap items-center justify-between gap-x-6 gap-y-3 px-5 py-4 sm:px-8 lg:px-10">
        <Link href="/clientes" className="flex items-center gap-3" aria-label="Hertz, ir al inicio">
          <span className="grid size-10 place-items-center rounded-xl bg-[#ffd400] text-lg font-black tracking-tighter text-zinc-950">
            H
          </span>
          <span className="text-lg font-black tracking-[0.16em]">HERTZ</span>
        </Link>

        <nav aria-label="Secciones" className="order-last flex w-full items-center gap-1 overflow-x-auto sm:order-none sm:w-auto">
          {enlaces.map((e) => {
            const activo = e.ruta === actual;
            return (
              <Link
                key={e.ruta}
                href={e.ruta}
                aria-current={activo ? "page" : undefined}
                className={`inline-flex h-10 items-center gap-2 rounded-xl px-4 text-sm font-semibold whitespace-nowrap transition-colors ${
                  activo ? "bg-white/15 text-white" : "text-zinc-300 hover:bg-white/10 hover:text-white"
                }`}
              >
                {esCliente && e.ruta === "/clientes" && <House className="size-4" aria-hidden="true" />}
                {e.etiqueta}
              </Link>
            );
          })}
        </nav>

        <div className="flex items-center gap-4">
          <div className="text-right">
            <p className="text-sm font-semibold">Hola, {nombreLimpio(nombre)}</p>
            <p className="text-xs text-zinc-400">
              {nombreDelRol(rol)}
              {sucursal ? ` · ${etiquetaSucursal(sucursal)}` : ""}
            </p>
          </div>
          <Button
            type="button"
            variant="outline"
            onClick={onSalir}
            className="h-10 rounded-xl border-white/20 bg-transparent px-4 text-white hover:bg-white/10 hover:text-white"
          >
            <LogOut className="size-4" aria-hidden="true" />
            <span className="hidden sm:inline">Cerrar sesión</span>
          </Button>
        </div>
      </div>
    </header>
  );
}
