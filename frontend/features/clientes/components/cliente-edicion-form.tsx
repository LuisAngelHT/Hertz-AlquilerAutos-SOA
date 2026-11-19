"use client";

import { useState, type FormEvent } from "react";
import { LoaderCircle, Save, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { ApiRequestError } from "@/lib/api-client";
import { ClienteFormFields } from "@/features/clientes/components/cliente-form-fields";
import { actualizarCliente } from "@/features/clientes/lib/cliente-api";
import type {
  Cliente,
  ClienteActualizacionRequest,
  ClienteFormValues,
} from "@/features/clientes/types/cliente";

interface ClienteEdicionFormProps {
  cliente: Cliente;
  token: string;
  onSaved: (cliente: Cliente) => void;
  onCancel: () => void;
  onAuthExpired: () => void;
}

export function ClienteEdicionForm({
  cliente,
  token,
  onSaved,
  onCancel,
  onAuthExpired,
}: ClienteEdicionFormProps) {
  const [values, setValues] = useState<ClienteFormValues>({
    tipoDocumento: cliente.tipoDocumento,
    numeroDocumento: cliente.numeroDocumento,
    nombres: cliente.nombres,
    apellidos: cliente.apellidos,
    fechaNacimiento: cliente.fechaNacimiento,
    email: cliente.email,
    telefono: cliente.telefono ?? "",
    licenciaConducir: cliente.licenciaConducir ?? "",
    vencimientoLicencia: cliente.vencimientoLicencia ?? "",
  });
  const [error, setError] = useState<string | null>(null);
  const [guardando, setGuardando] = useState(false);

  function handleChange(field: keyof ClienteFormValues, value: string) {
    setValues((current) => ({ ...current, [field]: value }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setGuardando(true);

    const request: ClienteActualizacionRequest = {
      nombres: values.nombres.trim(),
      apellidos: values.apellidos.trim(),
      email: values.email.trim(),
      telefono: values.telefono.trim() || null,
      licenciaConducir: values.licenciaConducir.trim().toUpperCase() || null,
      vencimientoLicencia: values.vencimientoLicencia || null,
    };

    try {
      const resultado = await actualizarCliente(
        token,
        cliente.idCliente,
        request,
      );
      onSaved(resultado);
    } catch (cause) {
      if (cause instanceof ApiRequestError && cause.status === 401) {
        onAuthExpired();
        return;
      }
      setError(
        cause instanceof Error
          ? cause.message
          : "No se pudieron guardar los cambios.",
      );
    } finally {
      setGuardando(false);
    }
  }

  return (
    <Card className="rounded-2xl border-0 bg-white shadow-sm ring-1 ring-zinc-200/80">
      <CardContent className="p-5 sm:p-7">
        <div className="mb-6 flex items-start justify-between gap-4">
          <div>
            <p className="text-xs font-semibold tracking-[0.12em] text-amber-700 uppercase">
              Editar ficha
            </p>
            <h3 className="mt-1 text-lg font-semibold text-zinc-950">
              {cliente.nombres} {cliente.apellidos}
            </h3>
            <p className="mt-1 text-sm text-zinc-500">
              {cliente.tipoDocumento} · {cliente.numeroDocumento}
            </p>
          </div>
          <Button
            type="button"
            variant="ghost"
            size="icon"
            aria-label="Cancelar edición"
            onClick={onCancel}
          >
            <X className="size-4" aria-hidden="true" />
          </Button>
        </div>

        <form className="space-y-6" onSubmit={handleSubmit}>
          <ClienteFormFields
            values={values}
            onChange={handleChange}
            incluirIdentidad={false}
          />

          {error && (
            <div
              role="alert"
              className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"
            >
              {error}
            </div>
          )}

          <div className="flex flex-col-reverse gap-3 border-t border-zinc-100 pt-5 sm:flex-row sm:justify-end">
            <Button
              type="button"
              variant="outline"
              className="h-11 rounded-xl"
              onClick={onCancel}
              disabled={guardando}
            >
              Cancelar
            </Button>
            <Button
              type="submit"
              disabled={guardando}
              className="h-11 rounded-xl bg-[#ffd400] px-5 font-semibold text-zinc-950 hover:bg-[#f2c900]"
            >
              {guardando ? (
                <>
                  <LoaderCircle
                    className="size-4 animate-spin"
                    aria-hidden="true"
                  />
                  Guardando...
                </>
              ) : (
                <>
                  <Save className="size-4" aria-hidden="true" />
                  Guardar cambios
                </>
              )}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}
