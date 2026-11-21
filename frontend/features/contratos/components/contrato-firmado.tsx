"use client";

import { useRef, useState } from "react";
import { Eye, FileCheck2, FileUp, LoaderCircle, RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { Sesion } from "@/features/flota/lib/session";
import {
  descargarDocumentoContrato,
  mensajeErrorContrato,
  subirDocumentoContrato,
} from "@/features/contratos/lib/contratos-api";
import type { Contrato, DocumentoContrato } from "@/features/contratos/types/contrato";
import { ApiRequestError } from "@/lib/api-client";

const TAMANO_MAXIMO = 5 * 1024 * 1024;
const TIPOS_ACEPTADOS = ".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png";

function formatoTamano(bytes: number): string {
  return bytes >= 1024 * 1024
    ? `${(bytes / (1024 * 1024)).toFixed(1)} MB`
    : `${Math.max(1, Math.round(bytes / 1024))} KB`;
}

function formatoFechaHora(fecha: string): string {
  const valor = new Date(fecha);
  return Number.isNaN(valor.getTime())
    ? fecha
    : new Intl.DateTimeFormat("es-PE", { dateStyle: "medium", timeStyle: "short" }).format(valor);
}

interface ContratoFirmadoProps {
  contrato: Contrato;
  sesion: Sesion;
  onSesionExpirada: () => void;
}

/**
 * Respaldo digital del contrato fisico: se firma en papel, se escanea y se sube aqui (PDF, JPG o PNG,
 * maximo 5 MB). Despues el personal lo puede abrir desde la tarjeta del contrato.
 */
export function ContratoFirmado({ contrato, sesion, onSesionExpirada }: ContratoFirmadoProps) {
  const [documento, setDocumento] = useState<DocumentoContrato | null>(contrato.documento ?? null);
  const [subiendo, setSubiendo] = useState(false);
  const [abriendo, setAbriendo] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);
  const selector = useRef<HTMLInputElement>(null);

  function manejarError(cause: unknown) {
    if (cause instanceof ApiRequestError && cause.status === 401) {
      onSesionExpirada();
      return;
    }
    setError(mensajeErrorContrato(cause));
  }

  async function elegirArchivo(archivo: File | undefined) {
    if (!archivo) return;
    setError(null);
    setAviso(null);
    if (archivo.size === 0) {
      setError("El archivo está vacío.");
      return;
    }
    if (archivo.size > TAMANO_MAXIMO) {
      setError(`El archivo pesa ${formatoTamano(archivo.size)} y el máximo es 5 MB.`);
      return;
    }
    setSubiendo(true);
    try {
      const actualizado = await subirDocumentoContrato(sesion.token, contrato.idContrato, archivo);
      setDocumento(actualizado.documento ?? null);
      setAviso("Contrato firmado guardado.");
    } catch (cause) {
      manejarError(cause);
    } finally {
      setSubiendo(false);
      if (selector.current) selector.current.value = "";   // permite volver a elegir el mismo archivo
    }
  }

  async function verContrato() {
    setError(null);
    setAviso(null);
    // La ventana se abre en el mismo clic (antes de esperar al servidor) para que el navegador no la bloquee.
    const ventana = window.open("", "_blank");
    setAbriendo(true);
    try {
      const archivo = await descargarDocumentoContrato(sesion.token, contrato.idContrato);
      const direccion = URL.createObjectURL(archivo);
      if (ventana) {
        ventana.location.href = direccion;
      } else {
        setError("El navegador bloqueó la ventana. Permite las ventanas emergentes e inténtalo de nuevo.");
      }
      setTimeout(() => URL.revokeObjectURL(direccion), 5 * 60 * 1000);
    } catch (cause) {
      ventana?.close();
      manejarError(cause);
    } finally {
      setAbriendo(false);
    }
  }

  return (
    <div className="border-t border-zinc-100 pt-4">
      <input
        ref={selector}
        type="file"
        accept={TIPOS_ACEPTADOS}
        className="sr-only"
        aria-label={`Elegir el contrato firmado escaneado de ${contrato.placa}`}
        onChange={(event) => void elegirArchivo(event.target.files?.[0])}
      />

      {documento ? (
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-emerald-50/70 px-4 py-3 ring-1 ring-emerald-200">
          <div className="flex min-w-0 items-start gap-2.5">
            <FileCheck2 className="mt-0.5 size-5 shrink-0 text-emerald-700" aria-hidden="true" />
            <div className="min-w-0">
              <p className="text-sm font-semibold text-emerald-900">Contrato firmado cargado</p>
              <p className="truncate text-xs text-emerald-800" title={documento.nombreArchivo}>
                {documento.nombreArchivo} · {formatoTamano(documento.tamanoBytes)}
              </p>
              <p className="text-xs text-emerald-800/80">
                Subido por {documento.subidoPor} el {formatoFechaHora(documento.subidoEn)}
              </p>
            </div>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button type="button" size="sm" onClick={() => void verContrato()} disabled={abriendo || subiendo}>
              {abriendo ? <LoaderCircle className="animate-spin" aria-hidden="true" /> : <Eye aria-hidden="true" />}
              Ver contrato firmado
            </Button>
            <Button
              type="button"
              size="sm"
              variant="outline"
              onClick={() => selector.current?.click()}
              disabled={subiendo || abriendo}
            >
              {subiendo ? <LoaderCircle className="animate-spin" aria-hidden="true" /> : <RefreshCw aria-hidden="true" />}
              Reemplazar
            </Button>
          </div>
        </div>
      ) : (
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-dashed border-amber-300 bg-amber-50/60 px-4 py-3">
          <div className="flex items-start gap-2.5">
            <FileUp className="mt-0.5 size-5 shrink-0 text-amber-700" aria-hidden="true" />
            <div>
              <p className="text-sm font-semibold text-amber-900">Falta subir el contrato firmado</p>
              <p className="text-xs text-amber-800/90">
                Escanea el contrato firmado en papel y súbelo como respaldo (PDF, JPG o PNG, máximo 5 MB).
              </p>
            </div>
          </div>
          <Button type="button" size="sm" onClick={() => selector.current?.click()} disabled={subiendo}>
            {subiendo ? <LoaderCircle className="animate-spin" aria-hidden="true" /> : <FileUp aria-hidden="true" />}
            {subiendo ? "Subiendo..." : "Subir contrato firmado"}
          </Button>
        </div>
      )}

      <div aria-live="polite">
        {error ? (
          <p role="alert" className="mt-2 text-xs font-medium text-red-700">
            {error}
          </p>
        ) : aviso ? (
          <p className="mt-2 text-xs font-medium text-emerald-700">{aviso}</p>
        ) : null}
      </div>
    </div>
  );
}
