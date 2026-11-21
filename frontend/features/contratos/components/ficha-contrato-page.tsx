"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft, LoaderCircle, Printer } from "lucide-react";
import { Button } from "@/components/ui/button";
import { VehiculoVistas, tipoCarroceria, type PinVista } from "@/features/contratos/components/vehiculo-diagrama";
import { mensajeErrorContrato, obtenerContrato } from "@/features/contratos/lib/contratos-api";
import {
  COMBUSTIBLES,
  GRUPOS_INVENTARIO,
  cuartos,
  estadosDeRevision,
  etiquetaCombustible,
  etiquetaEstadoItem,
  etiquetaGravedad,
  etiquetaTipoDano,
  etiquetaVista,
  etiquetaZona,
  formatoKm,
  formatoSoles,
} from "@/features/contratos/lib/revision";
import type { Contrato, DanoVehiculo, RevisionVehiculo } from "@/features/contratos/types/contrato";
import { etiquetaSucursal } from "@/features/flota/lib/constants";
import { consultarVehiculo, type Vehiculo } from "@/features/flota/lib/flota-api";
import { cerrarSesion } from "@/features/flota/lib/session";
import { notificarCambioSesion, useSesion } from "@/features/flota/lib/use-session";
import { consultarReserva, type Reserva } from "@/features/reservas/lib/reservas-api";
import { ApiRequestError } from "@/lib/api-client";

const LETRAS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
const ROLES_PERSONAL = ["AGENTE", "ADMINISTRADOR", "ADMINISTRADOR_SUCURSAL"];

function fechaHora(fecha: string | null | undefined): string {
  if (!fecha) return "—";
  const valor = new Date(fecha);
  return Number.isNaN(valor.getTime())
    ? fecha
    : new Intl.DateTimeFormat("es-PE", { dateStyle: "medium", timeStyle: "short" }).format(valor);
}

function fecha(fechaIso: string | null | undefined): string {
  if (!fechaIso) return "—";
  const valor = new Date(`${fechaIso}T12:00:00`);
  return Number.isNaN(valor.getTime()) ? fechaIso : new Intl.DateTimeFormat("es-PE", { dateStyle: "medium" }).format(valor);
}

function Medidor({ nivel }: { nivel: string | null }) {
  const lleno = nivel ? cuartos(nivel) : -1;
  return (
    <div className="flex items-center gap-1" aria-label={nivel ? etiquetaCombustible(nivel) : "Sin registro"}>
      <span className="text-[10px] font-bold text-zinc-500">E</span>
      {COMBUSTIBLES.map((c, i) => (
        <span key={c.value} className={`h-3 w-6 rounded-sm border border-zinc-400 ${i <= lleno ? "bg-amber-400" : "bg-white"}`} />
      ))}
      <span className="text-[10px] font-bold text-zinc-500">F</span>
      <span className="ml-1 text-xs text-zinc-700">{nivel ? etiquetaCombustible(nivel) : "—"}</span>
    </div>
  );
}

function ListaDanos({ titulo, danos, esDevolucion, letras }: { titulo: string; danos: DanoVehiculo[]; esDevolucion: boolean; letras: boolean }) {
  return (
    <div className="space-y-1">
      <p className="text-xs font-semibold text-zinc-700">{titulo}</p>
      {danos.length === 0 ? (
        <p className="text-xs text-zinc-500">{esDevolucion ? "Sin daños nuevos." : "Sin daños."}</p>
      ) : (
        <ol className="space-y-0.5 text-xs text-zinc-700">
          {danos.map((d, i) => (
            <li key={i}>
              <strong>{letras ? LETRAS[i % 26] : i + 1})</strong> {d.vista ? etiquetaVista(d.vista) : etiquetaZona(d.zona)} ·{" "}
              {d.tipo ? etiquetaTipoDano(d.tipo) : "Daño"} ({etiquetaGravedad(d.gravedad).toLowerCase()}): {d.descripcion}
              {esDevolucion && d.costoEstimado != null ? <strong> — {formatoSoles(d.costoEstimado)}</strong> : null}
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}

function pinesDe(entrega: RevisionVehiculo | null, devolucion: RevisionVehiculo | null): PinVista[] {
  const previos = (entrega?.danos ?? []).flatMap((d, i) =>
    d.vista && d.posX != null && d.posY != null
      ? [{ id: `a${i}`, vista: d.vista, x: d.posX, y: d.posY, numero: LETRAS[i % 26], tono: "previo" as const }]
      : [],
  );
  const nuevos = (devolucion?.danos ?? []).flatMap((d, i) =>
    d.vista && d.posX != null && d.posY != null
      ? [{ id: `n${i}`, vista: d.vista, x: d.posX, y: d.posY, numero: i + 1, tono: "nuevo" as const }]
      : [],
  );
  return [...previos, ...nuevos];
}

/** Ficha de entrega y recepción del vehículo: se ve en pantalla y se imprime (o guarda como PDF) para la firma en papel. */
export function FichaContratoPage() {
  const router = useRouter();
  const { id } = useParams<{ id: string }>();
  const { cargando, sesion } = useSesion();
  const [contrato, setContrato] = useState<Contrato | null>(null);
  const [vehiculo, setVehiculo] = useState<Vehiculo | null>(null);
  const [reserva, setReserva] = useState<Reserva | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!cargando && !sesion) router.replace("/");
  }, [cargando, sesion, router]);

  useEffect(() => {
    if (!sesion || !id) return;
    let activo = true;
    obtenerContrato(sesion.token, id)
      .then((datos) => {
        if (!activo) return;
        setContrato(datos);
        // Datos complementarios (marca, modelo, cliente): si no se pueden leer, la ficha igual se muestra.
        consultarVehiculo(sesion, datos.placa).then((v) => activo && setVehiculo(v)).catch(() => undefined);
        consultarReserva(sesion, datos.idReserva).then((r) => activo && setReserva(r)).catch(() => undefined);
      })
      .catch((cause: unknown) => {
        if (!activo) return;
        if (cause instanceof ApiRequestError && cause.status === 401) {
          cerrarSesion();
          notificarCambioSesion();
          router.replace("/");
          return;
        }
        setError(mensajeErrorContrato(cause));
      });
    return () => {
      activo = false;
    };
  }, [sesion, id, router]);

  if (cargando || !sesion || (!contrato && !error)) {
    return (
      <main className="grid min-h-svh place-items-center bg-[#fafaf9]">
        <p className="inline-flex items-center gap-2 text-sm text-zinc-500">
          <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
          Cargando la ficha...
        </p>
      </main>
    );
  }
  if (!ROLES_PERSONAL.includes(sesion.rol) || error || !contrato) {
    return (
      <main className="mx-auto max-w-xl p-8">
        <p role="alert" className="rounded-2xl border border-amber-200 bg-amber-50 p-5 text-sm text-amber-900">
          {error ?? "Tu cuenta no tiene acceso a las fichas de contratos."}
        </p>
        <Link href="/contratos" className="mt-4 inline-block text-sm font-semibold text-amber-700 underline">
          Volver a Contratos
        </Link>
      </main>
    );
  }

  const { entrega, devolucion } = contrato;
  const salida = estadosDeRevision(entrega);
  const regreso = estadosDeRevision(devolucion);
  const carroceria = tipoCarroceria(vehiculo?.categoria ?? reserva?.categoria);
  const aCargo = contrato.totalDanosACargoCliente ?? 0;
  const sinUbicar = (lista: DanoVehiculo[] | undefined) => (lista ?? []).filter((d) => !d.vista);

  return (
    <div className="min-h-svh bg-zinc-100 py-6 print:bg-white print:py-0">
      <style>{`@media print { @page { size: A4; margin: 9mm; } body { background: white !important; } }`}</style>

      <div className="mx-auto mb-4 flex max-w-[210mm] flex-wrap items-center justify-between gap-2 px-4 print:hidden">
        <Link href="/contratos" className="inline-flex items-center gap-1.5 text-sm font-semibold text-zinc-700 hover:text-zinc-950">
          <ArrowLeft className="size-4" aria-hidden="true" />
          Volver a Contratos
        </Link>
        <Button type="button" onClick={() => window.print()} className="h-10 rounded-xl bg-[#ffd400] px-4 font-bold text-zinc-950 hover:bg-[#f2c900]">
          <Printer aria-hidden="true" />
          Imprimir o guardar como PDF
        </Button>
      </div>

      <article className="mx-auto max-w-[210mm] space-y-4 bg-white p-8 text-zinc-900 shadow-sm print:max-w-none print:space-y-3 print:p-0 print:shadow-none">
        <header className="flex items-start justify-between gap-4 border-b-2 border-zinc-900 pb-3">
          <div>
            <p className="text-xs font-bold tracking-[0.2em] text-amber-700">HERTZ</p>
            <h1 className="text-lg font-bold uppercase leading-tight tracking-tight">Registro de entrega y recepción de vehículo</h1>
          </div>
          <div className="text-right text-xs text-zinc-600">
            <p className="font-mono text-sm font-bold text-zinc-900">Contrato N.º {contrato.idContrato.slice(0, 8).toUpperCase()}</p>
            <p>{contrato.estado === "VIGENTE" ? "Vigente" : "Finalizado"}</p>
            <p>Impreso: {fechaHora(new Date().toISOString())}</p>
          </div>
        </header>

        <section className="grid gap-3 text-xs sm:grid-cols-3 print:grid-cols-3">
          <div className="space-y-0.5 rounded-lg border border-zinc-300 p-2.5">
            <p className="font-bold uppercase text-zinc-600">Características generales</p>
            <p>Marca: <strong>{vehiculo?.marca ?? "—"}</strong></p>
            <p>Modelo: <strong>{vehiculo?.modelo ?? "—"}</strong>{vehiculo ? ` (${vehiculo.anio})` : ""}</p>
            <p>Tipo: <strong>{vehiculo?.categoria ?? reserva?.categoria ?? "—"}</strong></p>
            <p>Placa: <strong className="font-mono">{contrato.placa}</strong></p>
          </div>
          <div className="space-y-0.5 rounded-lg border border-zinc-300 p-2.5">
            <p className="font-bold uppercase text-zinc-600">Kilometraje</p>
            <p>De entrega: <strong>{entrega ? formatoKm(entrega.kilometraje) : "—"}</strong></p>
            <p>De devolución: <strong>{devolucion ? formatoKm(devolucion.kilometraje) : "—"}</strong></p>
            <p>Recorrido: <strong>{contrato.kilometrosRecorridos != null ? formatoKm(contrato.kilometrosRecorridos) : "—"}</strong></p>
          </div>
          <div className="space-y-0.5 rounded-lg border border-zinc-300 p-2.5">
            <p className="font-bold uppercase text-zinc-600">Lugar y fecha</p>
            <p>Entrega: <strong>{etiquetaSucursal(contrato.sucursalRecojo)}</strong></p>
            <p className="text-zinc-600">{fechaHora(entrega?.registradoEn ?? contrato.creadoEn)}</p>
            <p>Devolución: <strong>{etiquetaSucursal(contrato.sucursalDevolucion)}</strong></p>
            <p className="text-zinc-600">{devolucion ? fechaHora(devolucion.registradoEn) : `Prevista: ${fecha(contrato.fechaFinPrevista)}`}</p>
          </div>
        </section>

        <section className="grid gap-3 text-xs sm:grid-cols-2 print:grid-cols-2">
          <div className="space-y-0.5 rounded-lg border border-zinc-300 p-2.5">
            <p className="font-bold uppercase text-zinc-600">Cliente</p>
            <p><strong>{reserva?.clienteNombre ?? "—"}</strong></p>
            <p>{reserva?.clienteDocumento ?? ""}{reserva?.clienteTelefono ? ` · Tel. ${reserva.clienteTelefono}` : ""}</p>
          </div>
          <div className="space-y-0.5 rounded-lg border border-zinc-300 p-2.5">
            <p className="font-bold uppercase text-zinc-600">Funcionarios</p>
            <p>Entrega: <strong>{entrega?.registradoPor ?? contrato.registradoPor}</strong></p>
            <p>Recibe: <strong>{devolucion?.registradoPor ?? "—"}</strong></p>
          </div>
        </section>

        <section className="break-inside-avoid">
          <p className="mb-1.5 text-xs font-bold uppercase text-zinc-600">Inventario y control de condiciones generales</p>
          <div className="grid gap-2 sm:grid-cols-2 print:grid-cols-2">
            {GRUPOS_INVENTARIO.map((g) => (
              <table key={g.grupo} className="w-full break-inside-avoid border-collapse text-[11px]">
                <thead>
                  <tr className="bg-zinc-100">
                    <th className="border border-zinc-300 px-1.5 py-1 text-left">{g.grupo}</th>
                    <th className="w-14 border border-zinc-300 px-1 py-1">Antes</th>
                    <th className="w-14 border border-zinc-300 px-1 py-1">Después</th>
                  </tr>
                </thead>
                <tbody>
                  {g.items.map((item) => {
                    const antes = salida[item.codigo] ?? "OK";
                    const despues = devolucion ? (regreso[item.codigo] ?? "OK") : null;
                    const peor = despues != null && despues !== "OK" && antes === "OK";
                    return (
                      <tr key={item.codigo}>
                        <td className="border border-zinc-300 px-1.5 py-0.5">{item.etiqueta}</td>
                        <td className={`border border-zinc-300 px-1 py-0.5 text-center ${entrega ? "" : "text-zinc-300"}`}>
                          {entrega ? (antes === "OK" ? "✓" : etiquetaEstadoItem(antes)) : "—"}
                        </td>
                        <td className={`border border-zinc-300 px-1 py-0.5 text-center ${peor ? "bg-red-50 font-bold text-red-700" : ""}`}>
                          {despues == null ? "" : despues === "OK" ? "✓" : etiquetaEstadoItem(despues)}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            ))}
          </div>
        </section>

        <section className="break-inside-avoid space-y-2">
          <p className="text-xs font-bold uppercase text-zinc-600">Observaciones de orden general y estado</p>
          <VehiculoVistas tipo={carroceria} pines={pinesDe(entrega, devolucion)} columnas={3} />
          <p className="text-[11px] text-zinc-500">
            <span className="font-bold text-zinc-600">Gris con letra:</span> ya estaba al entregar ·{" "}
            <span className="font-bold text-red-600">Rojo con número:</span> daño nuevo al devolver.
          </p>
          <div className="grid gap-3 sm:grid-cols-2 print:grid-cols-2">
            <div className="space-y-1">
              <ListaDanos titulo="Daños al entregar (no se cobran)" danos={entrega?.danos ?? []} esDevolucion={false} letras />
              {sinUbicar(entrega?.danos).length > 0 && <p className="text-[11px] text-zinc-500">Los marcados sin ubicación son interiores u otras zonas.</p>}
            </div>
            <ListaDanos titulo="Daños nuevos al devolver (a cargo del cliente)" danos={devolucion?.danos ?? []} esDevolucion letras={false} />
          </div>
        </section>

        <section className="grid gap-3 text-xs sm:grid-cols-2 print:grid-cols-2">
          <div className="space-y-1 rounded-lg border border-zinc-300 p-2.5">
            <p className="font-bold uppercase text-zinc-600">Nivel de combustible</p>
            <p className="text-zinc-600">Al entregar</p>
            <Medidor nivel={entrega?.combustible ?? null} />
            <p className="text-zinc-600">Al devolver</p>
            <Medidor nivel={devolucion?.combustible ?? null} />
          </div>
          <div className="space-y-1 rounded-lg border border-zinc-300 p-2.5">
            <p className="font-bold uppercase text-zinc-600">Observaciones</p>
            <p>Entrega: {entrega?.observaciones ?? "—"}</p>
            <p>Devolución: {devolucion?.observaciones ?? "—"}</p>
            {devolucion && (
              <p className={`mt-1 rounded px-2 py-1 font-semibold ${aCargo > 0 ? "bg-red-50 text-red-800" : "bg-emerald-50 text-emerald-800"}`}>
                {aCargo > 0 ? `A cargo del cliente: ${formatoSoles(aCargo)}` : "Sin cargos para el cliente"}
                {devolucion.clienteConforme === false ? " · Cliente NO conforme" : devolucion.clienteConforme ? " · Cliente conforme" : ""}
              </p>
            )}
          </div>
        </section>

        <section className="grid grid-cols-2 gap-6 pt-8 text-center text-[11px] text-zinc-600 break-inside-avoid">
          {["Entrega", "Recepción"].map((acto) => (
            <div key={acto} className="grid grid-cols-2 gap-4">
              <div>
                <div className="border-t border-zinc-800 pt-1">Funcionario · {acto}</div>
              </div>
              <div>
                <div className="border-t border-zinc-800 pt-1">Cliente · {acto}</div>
              </div>
            </div>
          ))}
        </section>
      </article>
    </div>
  );
}
