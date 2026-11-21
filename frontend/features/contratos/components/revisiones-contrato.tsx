import Link from "next/link";
import { ClipboardCheck, Printer } from "lucide-react";
import {
  etiquetaCombustible,
  etiquetaEstadoItem,
  etiquetaGravedad,
  etiquetaItem,
  etiquetaTipoDano,
  etiquetaVista,
  etiquetaZona,
  formatoKm,
  formatoSoles,
} from "@/features/contratos/lib/revision";
import type { Contrato, RevisionVehiculo } from "@/features/contratos/types/contrato";

function formatoFechaHora(fecha: string): string {
  const valor = new Date(fecha);
  return Number.isNaN(valor.getTime())
    ? fecha
    : new Intl.DateTimeFormat("es-PE", { dateStyle: "medium", timeStyle: "short" }).format(valor);
}

function Revision({ titulo, revision, esDevolucion }: { titulo: string; revision: RevisionVehiculo; esDevolucion: boolean }) {
  // Los contratos hechos antes del inventario (o un servicio todavía sin actualizar) no traen la lista.
  const problemas = (revision.inventario ?? []).filter((i) => i.estado !== "OK");
  return (
    <div className="space-y-2 rounded-xl bg-zinc-50/80 p-3.5 ring-1 ring-zinc-200/70">
      <p className="text-xs font-semibold uppercase tracking-wide text-zinc-500">{titulo}</p>
      <p className="text-sm font-medium text-zinc-800">
        {formatoKm(revision.kilometraje)} · {etiquetaCombustible(revision.combustible)}
      </p>

      {revision.danos.length === 0 ? (
        <p className="text-xs text-zinc-500">{esDevolucion ? "Sin daños nuevos." : "Salió sin daños."}</p>
      ) : (
        <details className="group text-xs text-zinc-600">
          <summary className="cursor-pointer font-medium text-zinc-700">
            {revision.danos.length} daño{revision.danos.length === 1 ? "" : "s"}
            {esDevolucion ? " nuevo" + (revision.danos.length === 1 ? "" : "s") : " previo" + (revision.danos.length === 1 ? "" : "s")}
            <span className="text-zinc-400"> · ver detalle</span>
          </summary>
          <ul className="mt-2 space-y-1.5">
            {revision.danos.map((dano, i) => (
              <li key={i} className="rounded-lg bg-white px-2.5 py-1.5 ring-1 ring-zinc-200">
                <span className="font-medium text-zinc-800">{dano.vista ? etiquetaVista(dano.vista) : etiquetaZona(dano.zona)}</span>
                <span className="text-zinc-500">
                  {dano.tipo ? ` · ${etiquetaTipoDano(dano.tipo).toLowerCase()}` : ""} · {etiquetaGravedad(dano.gravedad).toLowerCase()}
                </span>
                <span className="block">{dano.descripcion}</span>
                {esDevolucion && dano.costoEstimado != null && (
                  <span className="block font-semibold text-red-700">Estimado: {formatoSoles(dano.costoEstimado)}</span>
                )}
              </li>
            ))}
          </ul>
        </details>
      )}

      {problemas.length > 0 && (
        <details className="text-xs text-zinc-600">
          <summary className="cursor-pointer font-medium text-zinc-700">
            {problemas.length} elemento{problemas.length === 1 ? "" : "s"} del inventario{" "}
            {esDevolucion ? "faltan o están dañados" : "falta" + (problemas.length === 1 ? "" : "n") + " o están dañados"}
            <span className="text-zinc-400"> · ver detalle</span>
          </summary>
          <ul className="mt-2 space-y-1">
            {problemas.map((item) => (
              <li key={item.item} className="rounded-lg bg-white px-2.5 py-1.5 ring-1 ring-zinc-200">
                <span className="font-medium text-zinc-800">{etiquetaItem(item.item)}</span>
                <span className="text-zinc-500"> · {etiquetaEstadoItem(item.estado).toLowerCase()}</span>
                {esDevolucion && item.costoEstimado != null && (
                  <span className="block font-semibold text-red-700">Estimado: {formatoSoles(item.costoEstimado)}</span>
                )}
              </li>
            ))}
          </ul>
        </details>
      )}

      {revision.observaciones && <p className="text-xs italic text-zinc-600">“{revision.observaciones}”</p>}
      <p className="text-[11px] text-zinc-400">
        {revision.registradoPor} · {formatoFechaHora(revision.registradoEn)}
      </p>
    </div>
  );
}

/** Revisión del vehículo al entregarlo y al recibirlo, con lo que asume el cliente por los daños nuevos. */
export function RevisionesContrato({ contrato }: { contrato: Contrato }) {
  const { entrega, devolucion } = contrato;

  if (!entrega && !devolucion) {
    return (
      <p className="border-t border-zinc-100 pt-4 text-xs leading-5 text-zinc-500">
        Este contrato es anterior a la revisión del vehículo: no tiene registro de cómo salió.
      </p>
    );
  }

  const aCargo = contrato.totalDanosACargoCliente ?? 0;

  return (
    <div className="space-y-3 border-t border-zinc-100 pt-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="flex items-center gap-2 text-xs font-semibold text-zinc-500">
          <ClipboardCheck className="size-4 text-zinc-400" aria-hidden="true" />
          Revisión del vehículo
        </p>
        <Link
          href={`/contratos/${contrato.idContrato}/ficha`}
          target="_blank"
          className="inline-flex items-center gap-1.5 rounded-lg px-2.5 py-1 text-xs font-semibold text-amber-800 ring-1 ring-amber-300 transition-colors hover:bg-amber-50"
        >
          <Printer className="size-3.5" aria-hidden="true" />
          Ver ficha / imprimir
        </Link>
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        {entrega ? (
          <Revision titulo="Entrega" revision={entrega} esDevolucion={false} />
        ) : (
          <p className="rounded-xl bg-zinc-50/80 p-3.5 text-xs text-zinc-500 ring-1 ring-zinc-200/70">
            Sin revisión de salida.
          </p>
        )}
        {devolucion ? (
          <Revision titulo="Devolución" revision={devolucion} esDevolucion />
        ) : (
          <p className="rounded-xl border border-dashed border-zinc-200 p-3.5 text-xs text-zinc-500">
            Pendiente: se registrará al devolver el vehículo.
          </p>
        )}
      </div>

      {devolucion && (
        <div
          className={`rounded-xl px-3.5 py-3 text-sm ring-1 ${
            aCargo > 0 ? "bg-red-50 text-red-900 ring-red-200" : "bg-emerald-50 text-emerald-900 ring-emerald-200"
          }`}
        >
          <p className="font-semibold">
            {aCargo > 0
              ? `A cargo del cliente por daños nuevos: ${formatoSoles(aCargo)}`
              : devolucion.danos.length > 0
                ? "Hay daños nuevos sin costo estimado todavía."
                : "Sin daños nuevos: el cliente no asume ningún cargo."}
          </p>
          <p className="mt-0.5 text-xs">
            {contrato.kilometrosRecorridos != null ? `Recorrió ${formatoKm(contrato.kilometrosRecorridos)}. ` : ""}
            {devolucion.clienteConforme === false
              ? "El cliente NO quedó conforme con lo registrado."
              : devolucion.clienteConforme
                ? "El cliente quedó conforme."
                : ""}
          </p>
        </div>
      )}
    </div>
  );
}
