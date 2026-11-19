"use client";

import { useEffect, useState, type FormEvent } from "react";
import Link from "next/link";
import { BadgeCheck, CalendarCheck, CarFront, LoaderCircle, MapPin, Plane, Repeat, Search, UserRound } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import type { LoginResponse } from "@/features/auth/lib/auth-api";
import { actualizarMiPerfil, obtenerMiPerfil } from "@/features/clientes/lib/cliente-api";
import type { Cliente } from "@/features/clientes/types/cliente";
import { CATEGORIAS, SUCURSALES, etiquetaCategoria, etiquetaSucursal } from "@/features/flota/lib/constants";
import {
  consultarDisponibilidad,
  SesionExpiradaError,
  type Disponibilidad,
} from "@/features/flota/lib/flota-api";
import { hoyEnLima } from "@/features/reservas/lib/constants";
import { ApiRequestError } from "@/lib/api-client";

const CAMPO =
  "h-12 w-full rounded-xl border border-zinc-200 bg-zinc-50/60 px-4 text-sm text-zinc-900 outline-none transition-colors focus-visible:border-amber-400 focus-visible:ring-3 focus-visible:ring-amber-300/40 disabled:opacity-50";

interface PortalClienteProps {
  sesion: LoginResponse;
  onAuthExpired: () => void;
}

/**
 * Portada del cliente: busca vehículos disponibles por sucursal, categoría y fechas y los reserva con
 * un clic, y administra su teléfono y su licencia. Es lo primero que ve el cliente al iniciar sesión.
 */
export function PortalCliente({ sesion, onAuthExpired }: PortalClienteProps) {
  return (
    <div>
      <section className="bg-[#111110] text-white">
        <div className="mx-auto max-w-7xl px-5 pb-14 pt-10 sm:px-8 lg:px-10">
          <p className="text-xs font-semibold tracking-[0.14em] text-[#ffd400] uppercase">Hertz Perú</p>
          <h1 className="mt-3 max-w-3xl text-3xl font-semibold tracking-tight sm:text-5xl">
            Hola, {sesion.nombre.split(" ")[0]}. ¿A dónde viajas?
          </h1>
          <p className="mt-3 max-w-2xl text-sm leading-6 text-zinc-300 sm:text-base">
            Elige tu sucursal, la categoría y las fechas. Te mostramos los vehículos libres y reservas en segundos.
          </p>
          <div className="mt-8 grid gap-3 sm:grid-cols-3">
            <Destacado icono={<Plane className="size-5" aria-hidden="true" />} titulo="En los aeropuertos del país" texto="Recoge en Lima, Cusco o Arequipa sin pasar por la ciudad." />
            <Destacado icono={<Repeat className="size-5" aria-hidden="true" />} titulo="Devuelve en otra sucursal" texto="Recoge en una sede y devuelve en otra, según tu ruta." />
            <Destacado icono={<CarFront className="size-5" aria-hidden="true" />} titulo="Un vehículo para cada viaje" texto="Económico, SUV, camioneta o premium." />
          </div>
        </div>
      </section>

      <div className="mx-auto max-w-7xl space-y-10 px-5 py-10 sm:px-8 lg:px-10">
        <Catalogo sesion={sesion} onAuthExpired={onAuthExpired} />
        <MiPerfil sesion={sesion} onAuthExpired={onAuthExpired} />
      </div>
    </div>
  );
}

function Destacado({ icono, titulo, texto }: { icono: React.ReactNode; titulo: string; texto: string }) {
  return (
    <div className="rounded-2xl bg-white/5 p-5 ring-1 ring-white/10">
      <span className="grid size-10 place-items-center rounded-xl bg-[#ffd400] text-zinc-950">{icono}</span>
      <p className="mt-3 text-sm font-semibold">{titulo}</p>
      <p className="mt-1 text-sm leading-5 text-zinc-400">{texto}</p>
    </div>
  );
}

// ----------------------------------------------------------------------------- catálogo

function Catalogo({ sesion, onAuthExpired }: PortalClienteProps) {
  const hoy = hoyEnLima();
  const [sucursal, setSucursal] = useState<string>(SUCURSALES[0].value);
  const [categoria, setCategoria] = useState<string>("ECONOMICO");
  const [desde, setDesde] = useState("");
  const [hasta, setHasta] = useState("");
  const [buscando, setBuscando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [resultado, setResultado] = useState<{ datos: Disponibilidad; busqueda: string } | null>(null);

  async function buscar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!desde || !hasta) {
      setError("Elige las fechas de inicio y de fin.");
      return;
    }
    setBuscando(true);
    setError(null);
    try {
      const datos = await consultarDisponibilidad(
        { ...sesion, sucursal: sesion.sucursal ?? null },
        { sucursal, categoria, fechaInicio: desde, fechaFin: hasta },
      );
      const params = new URLSearchParams({ nueva: "1", categoria, sucursal, desde, hasta });
      setResultado({ datos, busqueda: params.toString() });
    } catch (cause) {
      if (cause instanceof SesionExpiradaError) {
        onAuthExpired();
        return;
      }
      setResultado(null);
      setError(cause instanceof Error ? cause.message : "No se pudo consultar la disponibilidad.");
    } finally {
      setBuscando(false);
    }
  }

  return (
    <section aria-labelledby="titulo-catalogo" className="-mt-24 space-y-6">
      <Card className="rounded-3xl border-0 bg-white shadow-[0_18px_60px_-32px_rgba(24,24,27,0.35)] ring-1 ring-zinc-200/80">
        <CardContent className="p-5 sm:p-7">
          <h2 id="titulo-catalogo" className="text-lg font-semibold tracking-tight text-zinc-950">
            Busca tu vehículo
          </h2>
          <form onSubmit={buscar} className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-[1.6fr_1fr_1fr_1fr_auto] lg:items-end">
            <div className="space-y-2">
              <label htmlFor="cat-sucursal" className="text-sm font-semibold text-zinc-800">
                Sucursal de recojo
              </label>
              <select id="cat-sucursal" value={sucursal} onChange={(e) => setSucursal(e.target.value)} className={CAMPO}>
                {SUCURSALES.map((s) => (
                  <option key={s.value} value={s.value}>
                    {s.label}
                  </option>
                ))}
              </select>
            </div>
            <div className="space-y-2">
              <label htmlFor="cat-categoria" className="text-sm font-semibold text-zinc-800">
                Categoría
              </label>
              <select id="cat-categoria" value={categoria} onChange={(e) => setCategoria(e.target.value)} className={CAMPO}>
                {CATEGORIAS.map((c) => (
                  <option key={c.value} value={c.value}>
                    {c.label}
                  </option>
                ))}
              </select>
            </div>
            <div className="space-y-2">
              <label htmlFor="cat-desde" className="text-sm font-semibold text-zinc-800">
                Desde
              </label>
              <input
                id="cat-desde"
                type="date"
                min={hoy}
                value={desde}
                onChange={(e) => {
                  setDesde(e.target.value);
                  if (hasta && hasta < e.target.value) setHasta("");
                }}
                required
                className={CAMPO}
              />
            </div>
            <div className="space-y-2">
              <label htmlFor="cat-hasta" className="text-sm font-semibold text-zinc-800">
                Hasta
              </label>
              <input
                id="cat-hasta"
                type="date"
                min={desde}
                value={hasta}
                onChange={(e) => setHasta(e.target.value)}
                disabled={!desde}
                required
                className={CAMPO}
              />
            </div>
            <Button
              type="submit"
              disabled={buscando}
              className="h-12 rounded-xl bg-[#ffd400] px-6 text-sm font-bold text-zinc-950 shadow-[0_7px_18px_-9px_rgba(234,179,8,0.8)] hover:bg-[#f2c900]"
            >
              {buscando ? <LoaderCircle className="size-4 animate-spin" aria-hidden="true" /> : <Search className="size-4" aria-hidden="true" />}
              Buscar
            </Button>
          </form>
          {error && (
            <div role="alert" className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              {error}
            </div>
          )}
        </CardContent>
      </Card>

      {resultado && (
        <div aria-live="polite" className="space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <p className="text-sm font-medium text-zinc-600">
              {resultado.datos.cantidadDisponible === 0
                ? "No hay vehículos de esa categoría libres en esas fechas. Prueba con otras fechas u otra categoría."
                : `${resultado.datos.cantidadDisponible} ${resultado.datos.cantidadDisponible === 1 ? "vehículo disponible" : "vehículos disponibles"} en ${etiquetaSucursal(sucursal)}`}
            </p>
            {resultado.datos.cantidadDisponible > 0 && (
              <Link
                href={`/reservas?${resultado.busqueda}`}
                className="inline-flex h-11 items-center gap-2 rounded-xl bg-[#ffd400] px-5 text-sm font-bold text-zinc-950 hover:bg-[#f2c900]"
              >
                <CalendarCheck className="size-4" aria-hidden="true" />
                Reservar {etiquetaCategoria(categoria)}
              </Link>
            )}
          </div>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {resultado.datos.vehiculos.map((v) => (
              <Card key={v.placa} className="rounded-2xl border-0 bg-white shadow-[0_14px_44px_-30px_rgba(24,24,27,0.35)] ring-1 ring-zinc-200/80">
                <CardContent className="space-y-3 p-5">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-base font-semibold text-zinc-950">
                        {v.marca} {v.modelo}
                      </p>
                      <p className="text-xs text-zinc-400">Año {v.anio}</p>
                    </div>
                    <span className="rounded-full bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-700 ring-1 ring-emerald-200">
                      Disponible
                    </span>
                  </div>
                  <p className="flex items-center gap-2 text-sm text-zinc-600">
                    <MapPin className="size-4 text-zinc-400" aria-hidden="true" />
                    {etiquetaSucursal(v.sucursal)}
                  </p>
                  <p className="flex items-center gap-2 text-sm text-zinc-600">
                    <CarFront className="size-4 text-zinc-400" aria-hidden="true" />
                    {etiquetaCategoria(v.categoria)}
                  </p>
                  {/* Reserva ESTE vehículo (la placa viaja a la reserva); si alguien lo reserva antes, se avisa. */}
                  <Link
                    href={`/reservas?${resultado.busqueda}&placa=${encodeURIComponent(v.placa)}&vehiculo=${encodeURIComponent(`${v.marca} ${v.modelo} ${v.anio}`)}`}
                    className="inline-flex h-10 w-full items-center justify-center gap-2 rounded-xl border border-zinc-300 text-sm font-semibold text-zinc-900 transition-colors hover:bg-zinc-50"
                  >
                    <CalendarCheck className="size-4" aria-hidden="true" />
                    Reservar este vehículo
                  </Link>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      )}
    </section>
  );
}

// ----------------------------------------------------------------------------- perfil

function MiPerfil({ sesion, onAuthExpired }: PortalClienteProps) {
  const [cliente, setCliente] = useState<Cliente | null>(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [guardado, setGuardado] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [telefono, setTelefono] = useState("");
  const [licencia, setLicencia] = useState("");
  const [vencimiento, setVencimiento] = useState("");

  function aplicar(c: Cliente) {
    setCliente(c);
    setTelefono(c.telefono ?? "");
    setLicencia(c.licenciaConducir ?? "");
    setVencimiento(c.vencimientoLicencia ?? "");
  }

  useEffect(() => {
    let activo = true;
    obtenerMiPerfil(sesion.token)
      .then((c) => {
        if (activo) aplicar(c);
      })
      .catch((cause: unknown) => {
        if (!activo) return;
        if (cause instanceof ApiRequestError && cause.status === 401) {
          onAuthExpired();
          return;
        }
        setError(cause instanceof Error ? cause.message : "No se pudo cargar tu perfil.");
      })
      .finally(() => {
        if (activo) setCargando(false);
      });
    return () => {
      activo = false;
    };
  }, [sesion.token, onAuthExpired]);

  async function guardar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setGuardando(true);
    setError(null);
    setGuardado(false);
    try {
      aplicar(
        await actualizarMiPerfil(sesion.token, {
          telefono: telefono.trim(),
          licenciaConducir: licencia.trim().toUpperCase(),
          vencimientoLicencia: vencimiento,
        }),
      );
      setGuardado(true);
    } catch (cause) {
      if (cause instanceof ApiRequestError && cause.status === 401) {
        onAuthExpired();
        return;
      }
      setError(cause instanceof Error ? cause.message : "No se pudo guardar tu perfil.");
    } finally {
      setGuardando(false);
    }
  }

  return (
    <section aria-labelledby="titulo-perfil">
      <Card className="rounded-3xl border-0 bg-white shadow-sm ring-1 ring-zinc-200/80">
        <CardContent className="p-5 sm:p-7">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div className="flex items-center gap-3">
              <span className="grid size-11 place-items-center rounded-xl bg-[#fff4b8] text-zinc-900">
                <UserRound className="size-5" aria-hidden="true" />
              </span>
              <div>
                <h2 id="titulo-perfil" className="text-lg font-semibold tracking-tight text-zinc-950">
                  Mi perfil
                </h2>
                <p className="text-sm text-zinc-500">Mantén tu teléfono y tu licencia al día para poder reservar.</p>
              </div>
            </div>
            {cliente && (
              <span
                className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-semibold ${
                  cliente.licenciaVigente ? "bg-emerald-50 text-emerald-700" : "bg-amber-50 text-amber-800"
                }`}
              >
                <BadgeCheck className="size-3.5" aria-hidden="true" />
                {cliente.licenciaVigente ? "Licencia vigente" : "Licencia no vigente"}
              </span>
            )}
          </div>

          {cargando && (
            <p className="mt-6 inline-flex items-center gap-2 text-sm text-zinc-500">
              <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
              Cargando tu perfil...
            </p>
          )}

          {cliente && (
            <form onSubmit={guardar} className="mt-6 space-y-5">
              <dl className="grid gap-3 text-sm sm:grid-cols-3">
                <div>
                  <dt className="text-xs text-zinc-400">Nombre</dt>
                  <dd className="mt-1 font-medium text-zinc-800">
                    {cliente.nombres} {cliente.apellidos}
                  </dd>
                </div>
                <div>
                  <dt className="text-xs text-zinc-400">Documento</dt>
                  <dd className="mt-1 font-medium text-zinc-800">
                    {cliente.tipoDocumento} · {cliente.numeroDocumento}
                  </dd>
                </div>
                <div>
                  <dt className="text-xs text-zinc-400">Correo (es tu usuario de acceso)</dt>
                  <dd className="mt-1 break-all font-medium text-zinc-800">{cliente.email}</dd>
                </div>
              </dl>

              <div className="grid gap-4 sm:grid-cols-3">
                <div className="space-y-2">
                  <label htmlFor="perfil-tel" className="text-sm font-medium text-zinc-700">
                    Teléfono
                  </label>
                  <Input
                    id="perfil-tel"
                    type="tel"
                    value={telefono}
                    onChange={(e) => setTelefono(e.target.value)}
                    required
                    pattern="[+0-9 ()\-]{7,20}"
                    title="Solo números, espacios, + , ( ) y -. Entre 7 y 20 caracteres."
                    maxLength={20}
                    className="h-11 rounded-xl border-zinc-200 bg-white px-3 text-sm"
                  />
                </div>
                <div className="space-y-2">
                  <label htmlFor="perfil-lic" className="text-sm font-medium text-zinc-700">
                    Licencia de conducir
                  </label>
                  <Input
                    id="perfil-lic"
                    value={licencia}
                    onChange={(e) => setLicencia(e.target.value.toUpperCase())}
                    required
                    maxLength={20}
                    className="h-11 rounded-xl border-zinc-200 bg-white px-3 text-sm"
                  />
                </div>
                <div className="space-y-2">
                  <label htmlFor="perfil-venc" className="text-sm font-medium text-zinc-700">
                    Vencimiento de la licencia
                  </label>
                  <Input
                    id="perfil-venc"
                    type="date"
                    value={vencimiento}
                    onChange={(e) => setVencimiento(e.target.value)}
                    required
                    min={hoyEnLima()}
                    className="h-11 rounded-xl border-zinc-200 bg-white px-3 text-sm"
                  />
                </div>
              </div>

              {error && (
                <div role="alert" className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                  {error}
                </div>
              )}
              {guardado && (
                <div role="status" className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
                  Tus datos se guardaron.
                </div>
              )}

              <Button
                type="submit"
                disabled={guardando}
                className="h-11 rounded-xl bg-[#ffd400] px-5 font-semibold text-zinc-950 hover:bg-[#f2c900]"
              >
                {guardando && <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />}
                Guardar cambios
              </Button>
            </form>
          )}

          {!cargando && !cliente && error && (
            <div role="alert" className="mt-5 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              {error}
            </div>
          )}
        </CardContent>
      </Card>
    </section>
  );
}
