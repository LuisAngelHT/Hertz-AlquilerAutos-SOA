import {
  ArrowUpRight,
  CarFront,
  CircleCheck,
  KeyRound,
  ShieldCheck,
  Sparkles,
} from "lucide-react";
import { LoginForm } from "@/features/auth/components/login-form";

function Brand() {
  return (
    <div className="flex items-center gap-3" aria-label="Hertz">
      <span className="grid size-11 place-items-center rounded-xl bg-[#ffd400] text-xl font-black tracking-tighter text-zinc-950">
        H
      </span>
      <span className="text-xl font-black tracking-[0.16em] text-white">
        HERTZ
      </span>
    </div>
  );
}

export function LoginPage() {
  return (
    <main className="min-h-svh bg-[#fafaf9] lg:grid lg:grid-cols-[1.05fr_0.95fr]">
      <section className="relative isolate flex min-h-[410px] flex-col overflow-hidden bg-[#111110] px-6 py-7 text-white sm:px-10 sm:py-9 lg:min-h-svh lg:px-14 lg:py-12 xl:px-20">
        <div
          aria-hidden="true"
          className="pointer-events-none absolute inset-0 -z-10 opacity-60 [background-image:radial-gradient(#ffffff18_1px,transparent_1px)] [background-size:22px_22px]"
        />
        <div
          aria-hidden="true"
          className="pointer-events-none absolute -right-36 top-24 -z-10 size-[420px] rounded-full bg-[#ffd400]/10 blur-[100px]"
        />

        <Brand />

        <div className="mx-auto flex w-full max-w-xl flex-1 flex-col justify-center py-12 lg:py-20">
          <div className="mb-6 inline-flex w-fit items-center gap-2 rounded-full border border-white/15 bg-white/[0.06] px-3.5 py-2 text-xs font-semibold tracking-wide text-zinc-200">
            <Sparkles className="size-3.5 text-[#ffd400]" aria-hidden="true" />
            TU CAMINO EMPIEZA AQUÍ
          </div>

          <h1 className="max-w-lg text-4xl leading-[1.08] font-semibold tracking-tight sm:text-5xl lg:text-6xl">
            Cada viaje,
            <br />
            una nueva
            <br />
            <span className="text-[#ffd400]">posibilidad.</span>
          </h1>
          <p className="mt-6 max-w-md text-base leading-7 text-zinc-400 sm:text-lg">
            Accede a tu cuenta y continúa donde lo dejaste. Tu próximo destino
            está más cerca.
          </p>

          <div className="relative mt-10 flex min-h-36 items-center justify-center overflow-hidden rounded-3xl border border-white/10 bg-white/[0.035] sm:mt-14 sm:min-h-48">
            <div
              aria-hidden="true"
              className="absolute inset-x-8 bottom-8 h-px bg-gradient-to-r from-transparent via-[#ffd400]/70 to-transparent"
            />
            <div
              aria-hidden="true"
              className="absolute bottom-8 left-[22%] size-2 rounded-full bg-[#ffd400] shadow-[0_0_18px_5px_#ffd40066]"
            />
            <div
              aria-hidden="true"
              className="absolute right-[22%] bottom-8 size-2 rounded-full bg-[#ffd400] shadow-[0_0_18px_5px_#ffd40066]"
            />
            <CarFront
              className="size-24 stroke-[1.1] text-[#ffd400] sm:size-32"
              aria-hidden="true"
            />
            <span className="absolute top-5 right-5 rounded-full border border-white/10 bg-black/30 px-3 py-1.5 text-[10px] font-medium tracking-[0.18em] text-zinc-400 uppercase">
              LISTOS PARA SALIR
            </span>
          </div>

          <div className="mt-8 flex flex-wrap gap-x-6 gap-y-3 text-xs text-zinc-400 sm:text-sm">
            <span className="inline-flex items-center gap-2">
              <ShieldCheck className="size-4 text-[#ffd400]" aria-hidden="true" />
              Acceso protegido
            </span>
            <span className="inline-flex items-center gap-2">
              <KeyRound className="size-4 text-[#ffd400]" aria-hidden="true" />
              Tu cuenta, siempre a mano
            </span>
          </div>
        </div>

        <p className="hidden text-xs text-zinc-500 lg:block">
          © Hertz. Todos los derechos reservados.
        </p>
      </section>

      <section className="flex min-h-[560px] flex-col px-5 py-8 sm:px-10 lg:min-h-svh lg:px-14 lg:py-12 xl:px-20">
        <div className="hidden items-center justify-end text-sm text-zinc-500 lg:flex">
          Portal de acceso
          <ArrowUpRight className="ml-1.5 size-4" aria-hidden="true" />
        </div>

        <div className="mx-auto flex w-full max-w-md flex-1 flex-col justify-center py-10 lg:py-16">
          <div className="mb-8 lg:hidden">
            <span className="inline-grid size-10 place-items-center rounded-xl bg-[#ffd400] text-lg font-black text-zinc-950">
              H
            </span>
          </div>

          <div className="mb-8">
            <div className="mb-4 grid size-12 place-items-center rounded-2xl bg-[#fff4b8] text-zinc-900">
              <CarFront className="size-6" aria-hidden="true" />
            </div>
            <p className="mb-2 text-sm font-semibold tracking-[0.12em] text-zinc-500 uppercase">
              Bienvenido de nuevo
            </p>
            <h2 className="text-3xl font-semibold tracking-tight text-zinc-950 sm:text-4xl">
              Inicia sesión
            </h2>
            <p className="mt-3 text-sm leading-6 text-zinc-500 sm:text-base">
              Ingresa tus credenciales para acceder a tu cuenta.
            </p>
          </div>

          <LoginForm />

          <div className="mt-7 flex items-start gap-3 rounded-2xl border border-zinc-200/80 bg-white/70 p-4">
            <CircleCheck
              className="mt-0.5 size-4 shrink-0 text-amber-600"
              aria-hidden="true"
            />
            <p className="text-xs leading-5 text-zinc-500">
              ¿Tienes problemas para ingresar? Comunícate con el administrador
              de tu cuenta.
            </p>
          </div>
        </div>

        <p className="text-center text-xs text-zinc-400 lg:text-right">
          Hertz · Acceso seguro
        </p>
      </section>
    </main>
  );
}
