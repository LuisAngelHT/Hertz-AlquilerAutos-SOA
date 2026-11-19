"use client";

import { useState, type FormEvent, type KeyboardEvent } from "react";
import Link from "next/link";
import {
  ArrowRight,
  Check,
  CircleAlert,
  Eye,
  EyeOff,
  LoaderCircle,
  LockKeyhole,
  Mail,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  loginWithCredentials,
  type LoginResponse,
} from "@/features/auth/lib/auth-api";

const SESSION_KEY = "hertz-auth-session";

// text-base (16px) evita el zoom automático de iOS al enfocar el campo.
const inputClass =
  "h-12 rounded-xl border-zinc-400 bg-white text-base md:text-base placeholder:text-zinc-500 focus-visible:border-ring focus-visible:ring-ring/40";

export function LoginForm() {
  const [correo, setCorreo] = useState("");
  const [password, setPassword] = useState("");
  const [mostrarPassword, setMostrarPassword] = useState(false);
  const [mayusActivas, setMayusActivas] = useState(false);
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

  function handlePasswordKey(event: KeyboardEvent<HTMLInputElement>) {
    setMayusActivas(event.getModifierState("CapsLock"));
  }

  if (sesion) {
    return (
      <div role="status" className="rounded-2xl bg-white p-6 ring-1 ring-zinc-200">
        <div className="mb-5 grid size-12 place-items-center rounded-full bg-primary text-primary-foreground">
          <Check className="size-6" strokeWidth={2.5} aria-hidden="true" />
        </div>
        <h2 className="text-2xl font-semibold tracking-tight text-zinc-950">
          ¡Hola, {sesion.nombre}!
        </h2>
        <p className="mt-2 text-sm leading-6 text-zinc-600">
          Iniciaste sesión como{" "}
          <span className="font-medium text-zinc-900">{sesion.rol}</span>.
        </p>
        <Button
          type="button"
          variant="outline"
          className="mt-6 h-12 w-full rounded-xl border-zinc-300 text-base"
          onClick={handleLogout}
        >
          Cerrar sesión
        </Button>
      </div>
    );
  }

  return (
    <form className="space-y-5" onSubmit={handleSubmit} noValidate={false}>
      <div className="space-y-2">
        <label htmlFor="correo" className="text-sm font-semibold text-zinc-900">
          Correo electrónico
        </label>
        <div className="relative">
          <Mail
            className="pointer-events-none absolute top-1/2 left-4 size-[18px] -translate-y-1/2 text-zinc-500"
            aria-hidden="true"
          />
          <Input
            id="correo"
            name="correo"
            type="email"
            autoComplete="username"
            inputMode="email"
            autoCapitalize="none"
            spellCheck={false}
            placeholder="nombre@correo.com"
            value={correo}
            onChange={(event) => setCorreo(event.target.value)}
            required
            maxLength={120}
            disabled={cargando}
            aria-invalid={error ? true : undefined}
            aria-describedby={error ? "login-error" : undefined}
            className={`${inputClass} pl-11`}
          />
        </div>
      </div>

      <div className="space-y-2">
        <div className="flex items-center justify-between gap-3">
          <label
            htmlFor="password"
            className="text-sm font-semibold text-zinc-900"
          >
            Contraseña
          </label>
          {/* Ajusta la ruta a tu flujo de recuperación */}
          <Link
            href=""
            className="rounded-sm text-sm font-medium text-zinc-700 underline underline-offset-4 hover:text-zinc-950 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"
          >
            ¿Olvidaste tu contraseña?
          </Link>
        </div>
        <div className="relative">
          <LockKeyhole
            className="pointer-events-none absolute top-1/2 left-4 size-[18px] -translate-y-1/2 text-zinc-500"
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
            onKeyDown={handlePasswordKey}
            onKeyUp={handlePasswordKey}
            onBlur={() => setMayusActivas(false)}
            required
            disabled={cargando}
            aria-invalid={error ? true : undefined}
            aria-describedby={error ? "login-error" : undefined}
            className={`${inputClass} pr-14 pl-11`}
          />
          <button
            type="button"
            onClick={() => setMostrarPassword((visible) => !visible)}
            aria-label={
              mostrarPassword ? "Ocultar contraseña" : "Mostrar contraseña"
            }
            aria-pressed={mostrarPassword}
            className="absolute top-1/2 right-1.5 grid size-10 -translate-y-1/2 place-items-center rounded-lg text-zinc-500 transition-colors hover:bg-zinc-100 hover:text-zinc-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"
          >
            {mostrarPassword ? (
              <EyeOff className="size-[18px]" aria-hidden="true" />
            ) : (
              <Eye className="size-[18px]" aria-hidden="true" />
            )}
          </button>
        </div>
        {mayusActivas && (
          <p role="status" className="text-xs font-medium text-amber-800">
            Bloq Mayús está activado.
          </p>
        )}
      </div>

      {error && (
        <div
          id="login-error"
          role="alert"
          className="flex items-start gap-3 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm leading-5 text-red-800"
        >
          <CircleAlert className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
          {error}
        </div>
      )}

      <Button
        type="submit"
        disabled={cargando}
        className="h-12 w-full rounded-xl bg-primary text-base font-semibold text-primary-foreground transition-colors hover:bg-[#f2c900]"
      >
        {cargando ? (
          <>
            <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
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
  );
}