"use client";

import { useState, type FormEvent } from "react";
import {
  ArrowRight,
  Eye,
  EyeOff,
  LoaderCircle,
  LockKeyhole,
  Mail,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  loginWithCredentials,
  type LoginResponse,
} from "@/features/auth/lib/auth-api";

const SESSION_KEY = "hertz-auth-session";

export function LoginForm() {
  const [correo, setCorreo] = useState("");
  const [password, setPassword] = useState("");
  const [mostrarPassword, setMostrarPassword] = useState(false);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [sesion, setSesion] = useState<LoginResponse | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setCargando(true);

    try {
      const resultado = await loginWithCredentials(correo, password);
      window.sessionStorage.setItem(SESSION_KEY, JSON.stringify(resultado));
      setSesion(resultado);
    } catch (cause) {
      setError(
        cause instanceof TypeError
          ? "No se pudo conectar con el servidor. Inténtalo de nuevo más tarde."
          : cause instanceof Error
            ? cause.message
            : "No se pudo iniciar sesión. Inténtalo de nuevo.",
      );
    } finally {
      setCargando(false);
    }
  }

  function handleLogout() {
    window.sessionStorage.removeItem(SESSION_KEY);
    setSesion(null);
    setPassword("");
    setError(null);
  }

  if (sesion) {
    return (
      <Card className="rounded-3xl border-0 bg-white shadow-[0_18px_60px_-32px_rgba(24,24,27,0.28)] ring-1 ring-zinc-200/80">
        <CardContent className="p-6 sm:p-8">
          <div className="mb-5 grid size-14 place-items-center rounded-2xl bg-[#fff4b8] text-zinc-950">
            <span className="text-2xl font-semibold">✓</span>
          </div>
          <p className="text-sm font-semibold text-amber-700">Acceso correcto</p>
          <h3 className="mt-2 text-2xl font-semibold tracking-tight text-zinc-950">
            ¡Hola, {sesion.nombre}!
          </h3>
          <p className="mt-2 text-sm leading-6 text-zinc-500">
            Iniciaste sesión como{" "}
            <span className="font-medium text-zinc-800">{sesion.rol}</span>.
          </p>
          <Button
            type="button"
            variant="outline"
            className="mt-7 h-11 w-full rounded-xl border-zinc-200"
            onClick={handleLogout}
          >
            Cerrar sesión
          </Button>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="rounded-3xl border-0 bg-white shadow-[0_18px_60px_-32px_rgba(24,24,27,0.28)] ring-1 ring-zinc-200/80">
      <CardContent className="p-5 sm:p-8">
        <form className="space-y-5" onSubmit={handleSubmit}>
          <div className="space-y-2">
            <label
              htmlFor="correo"
              className="text-sm font-semibold text-zinc-800"
            >
              Correo electrónico
            </label>
            <div className="relative">
              <Mail
                className="pointer-events-none absolute top-1/2 left-4 size-[18px] -translate-y-1/2 text-zinc-400"
                aria-hidden="true"
              />
              <Input
                id="correo"
                name="correo"
                type="email"
                autoComplete="email"
                inputMode="email"
                placeholder="nombre@correo.com"
                value={correo}
                onChange={(event) => setCorreo(event.target.value)}
                required
                maxLength={120}
                disabled={cargando}
                className="h-12 rounded-xl border-zinc-200 bg-zinc-50/60 pl-11 text-sm placeholder:text-zinc-400 focus-visible:border-amber-400 focus-visible:ring-amber-300/40"
              />
            </div>
          </div>

          <div className="space-y-2">
            <div className="flex items-center justify-between gap-3">
              <label
                htmlFor="password"
                className="text-sm font-semibold text-zinc-800"
              >
                Contraseña
              </label>
              <span className="text-xs text-zinc-400">Acceso de cuenta</span>
            </div>
            <div className="relative">
              <LockKeyhole
                className="pointer-events-none absolute top-1/2 left-4 size-[18px] -translate-y-1/2 text-zinc-400"
                aria-hidden="true"
              />
              <Input
                id="password"
                name="password"
                type={mostrarPassword ? "text" : "password"}
                autoComplete="current-password"
                placeholder="Ingresa tu contraseña"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                required
                disabled={cargando}
                className="h-12 rounded-xl border-zinc-200 bg-zinc-50/60 pr-12 pl-11 text-sm placeholder:text-zinc-400 focus-visible:border-amber-400 focus-visible:ring-amber-300/40"
              />
              <button
                type="button"
                onClick={() => setMostrarPassword((visible) => !visible)}
                aria-label={
                  mostrarPassword
                    ? "Ocultar contraseña"
                    : "Mostrar contraseña"
                }
                aria-pressed={mostrarPassword}
                className="absolute top-1/2 right-3 grid size-8 -translate-y-1/2 place-items-center rounded-lg text-zinc-400 transition-colors hover:bg-zinc-100 hover:text-zinc-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amber-500"
              >
                {mostrarPassword ? (
                  <EyeOff className="size-[18px]" aria-hidden="true" />
                ) : (
                  <Eye className="size-[18px]" aria-hidden="true" />
                )}
              </button>
            </div>
          </div>

          {error && (
            <div
              role="alert"
              className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm leading-5 text-red-700"
            >
              {error}
            </div>
          )}

          <Button
            type="submit"
            disabled={cargando}
            className="h-12 w-full rounded-xl bg-[#ffd400] text-sm font-bold text-zinc-950 shadow-[0_7px_18px_-9px_rgba(234,179,8,0.8)] transition-all hover:bg-[#f2c900] hover:shadow-[0_10px_22px_-10px_rgba(234,179,8,0.9)]"
          >
            {cargando ? (
              <>
                <LoaderCircle
                  className="size-4 animate-spin"
                  aria-hidden="true"
                />
                Verificando acceso...
              </>
            ) : (
              <>
                Iniciar sesión
                <ArrowRight
                  className="size-4 transition-transform group-hover/button:translate-x-0.5"
                  aria-hidden="true"
                />
              </>
            )}
          </Button>
        </form>

        <div className="my-6 flex items-center gap-3">
          <div className="h-px flex-1 bg-zinc-100" />
          <span className="text-[11px] font-medium tracking-[0.12em] text-zinc-400 uppercase">
            Hertz
          </span>
          <div className="h-px flex-1 bg-zinc-100" />
        </div>

        <p className="text-center text-xs leading-5 text-zinc-400">
          Al continuar, accedes de forma segura a los servicios de tu cuenta.
        </p>
      </CardContent>
    </Card>
  );
}
