import {
  ArrowUpRight,
  CarFront,
  KeyRound,
  ShieldCheck,
} from "lucide-react";
import { LoginForm } from "@/features/auth/components/login-form";

function Brand() {
  return (
    <div className="flex items-center gap-4">
      <img
        src="/images/logo_isotipo.png"
        alt="Hertz"
        className="h-14 w-auto object-contain sm:h-16"
      />

      <span className="text-xs font-semibold tracking-[0.16em] text-zinc-300 sm:text-xl">
        TU CAMINO EMPIEZA AQUÍ
      </span>
    </div>
  );
}

export function LoginPage() {
  return (
    <main className="min-h-svh bg-[#fafaf9] lg:grid lg:grid-cols-[1.05fr_0.95fr]">
      <section className="relative isolate hidden min-h-svh flex-col overflow-hidden bg-[#111110] px-6 py-7 text-white lg:flex lg:px-14 lg:py-12 xl:px-20">
        <div
          aria-hidden="true"
          className="pointer-events-none absolute inset-0 -z-10 opacity-60 [background-image:radial-gradient(#ffffff18_1px,transparent_1px)] [background-size:22px_22px]"
        />
        <div
          aria-hidden="true"
          className="pointer-events-none absolute -right-36 top-24 -z-10 size-[420px] rounded-full bg-[#ffd400]/10 blur-[100px]"
        />

        <div className="mx-auto flex w-full max-w-xl flex-col py-0 lg:flex-1 lg:justify-center lg:py-20">
          <Brand />

          <br />
          <br />

          <h1 className="max-w-2xl text-4xl leading-[1.08] font-bold tracking-tight sm:text-5xl lg:text-7xl">
            Cada viaje, una
            <br />
            <span className="whitespace-nowrap">
              nueva <span className="text-[#ffd400]">oportunidad.</span>
            </span>
          </h1>
          <p className="mt-4 max-w-md text-sm leading-6 text-zinc-400 sm:mt-6 sm:text-lg sm:leading-7">
            Accede a tu cuenta y continúa donde lo dejaste. <br /> Tu próximo destino
            está más cerca.
          </p>

          <div className="relative mt-6 flex min-h-24 items-center justify-center overflow-hidden rounded-3xl border border-white/10 bg-white/[0.035] sm:mt-14 sm:min-h-48">
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
              className="size-20 stroke-[1.1] text-[#ffd400] sm:size-32"
              aria-hidden="true"
            />
          </div>

          <div className="mt-5 flex flex-wrap gap-x-6 gap-y-3 text-xs text-zinc-400 sm:mt-8 sm:text-sm">
            <span className="inline-flex items-center gap-2">
              <CarFront className="size-4 text-[#ffd400]" aria-hidden="true" />
              Viajes a tu ritmo
            </span>
            <span className="inline-flex items-center gap-2">
              <KeyRound className="size-4 text-[#ffd400]" aria-hidden="true" />
              Reserva de forma sencilla
            </span>
            <span className="inline-flex items-center gap-2">
              <ShieldCheck className="size-4 text-[#ffd400]" aria-hidden="true" />
              Seguridad en cada trayecto
            </span>
          </div>
        </div>

        <p className="hidden text-sm text-zinc-500 lg:block">
          © Hertz. Todos los derechos reservados.
        </p>
      </section>

      <section className="flex min-h-svh flex-col px-6 py-6 sm:px-10 lg:px-14 lg:py-12 xl:px-20">
        <div className="hidden items-center justify-end text-sm text-zinc-500 lg:flex">
          Portal de acceso
          <ArrowUpRight className="ml-1.5 size-4" aria-hidden="true" />
        </div>

        <div className="mx-auto flex w-full max-w-md flex-1 flex-col justify-center py-4 lg:py-16">
          <div className="mb-7 sm:mb-8">
            <div className="mb-4 flex justify-center lg:justify-start">
              <img
                src="/images/logo_principal.png"
                alt="Hertz"
                className="h-16 w-auto object-contain sm:h-18"
              />
            </div>
            <p className="mt-3 text-center text-sm leading-6 text-zinc-500 sm:text-base lg:text-left">
              Ingresa tus credenciales para acceder a tu cuenta.
            </p>
          </div>

          <LoginForm />
        </div>
      </section>
    </main>
  );
}
