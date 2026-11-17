import { BadgeCheck, IdCard, Mail, Phone, UserRound } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import type { Cliente } from "@/features/clientes/types/cliente";

export function ClienteResumen({ cliente }: { cliente: Cliente }) {
  return (
    <Card className="rounded-2xl border-0 bg-white shadow-sm ring-1 ring-zinc-200/80">
      <CardContent className="p-5 sm:p-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="grid size-12 place-items-center rounded-2xl bg-[#fff4b8] text-zinc-900">
              <UserRound className="size-5" aria-hidden="true" />
            </div>
            <div>
              <h3 className="text-lg font-semibold text-zinc-950">
                {cliente.nombres} {cliente.apellidos}
              </h3>
              <p className="mt-0.5 text-sm text-zinc-500">
                Cliente registrado
              </p>
            </div>
          </div>
          <span
            className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-semibold ${
              cliente.licenciaVigente
                ? "bg-emerald-50 text-emerald-700"
                : "bg-zinc-100 text-zinc-600"
            }`}
          >
            <BadgeCheck className="size-3.5" aria-hidden="true" />
            {cliente.licenciaVigente ? "Licencia vigente" : "Sin licencia vigente"}
          </span>
        </div>

        <dl className="mt-6 grid gap-4 border-t border-zinc-100 pt-5 sm:grid-cols-2">
          <div className="flex items-start gap-3">
            <IdCard
              className="mt-0.5 size-4 shrink-0 text-zinc-400"
              aria-hidden="true"
            />
            <div>
              <dt className="text-xs text-zinc-400">Documento</dt>
              <dd className="mt-1 text-sm font-medium text-zinc-800">
                {cliente.tipoDocumento} · {cliente.numeroDocumento}
              </dd>
            </div>
          </div>
          <div className="flex items-start gap-3">
            <Mail
              className="mt-0.5 size-4 shrink-0 text-zinc-400"
              aria-hidden="true"
            />
            <div>
              <dt className="text-xs text-zinc-400">Correo electrónico</dt>
              <dd className="mt-1 break-all text-sm font-medium text-zinc-800">
                {cliente.email}
              </dd>
            </div>
          </div>
          <div className="flex items-start gap-3">
            <Phone
              className="mt-0.5 size-4 shrink-0 text-zinc-400"
              aria-hidden="true"
            />
            <div>
              <dt className="text-xs text-zinc-400">Teléfono</dt>
              <dd className="mt-1 text-sm font-medium text-zinc-800">
                {cliente.telefono || "No registrado"}
              </dd>
            </div>
          </div>
          <div className="flex items-start gap-3">
            <IdCard
              className="mt-0.5 size-4 shrink-0 text-zinc-400"
              aria-hidden="true"
            />
            <div>
              <dt className="text-xs text-zinc-400">Licencia de conducir</dt>
              <dd className="mt-1 text-sm font-medium text-zinc-800">
                {cliente.licenciaConducir || "No registrada"}
                {cliente.vencimientoLicencia &&
                  ` · vence ${cliente.vencimientoLicencia}`}
              </dd>
            </div>
          </div>
        </dl>
      </CardContent>
    </Card>
  );
}
