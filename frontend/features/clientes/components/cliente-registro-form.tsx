"use client";

import { useState, type FormEvent } from "react";
import { ArrowRight, CheckCircle2, LoaderCircle, UserPlus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { ApiRequestError } from "@/lib/api-client";
import { ClienteFormFields } from "@/features/clientes/components/cliente-form-fields";
import { ClienteResumen } from "@/features/clientes/components/cliente-resumen";
import { registrarCliente } from "@/features/clientes/lib/cliente-api";
import {
  FORMULARIO_CLIENTE_VACIO,
  type Cliente,
  type ClienteFormValues,
  type ClienteRegistroRequest,
} from "@/features/clientes/types/cliente";

interface ClienteRegistroFormProps {
  token?: string;
  modalidad: "presencial" | "autoregistro";
  onAuthExpired?: () => void;
  onPublicSuccess?: (cliente: Cliente) => void;
}

export function ClienteRegistroForm({
  token,
  modalidad,
  onAuthExpired,
  onPublicSuccess,
}: ClienteRegistroFormProps) {
  const [values, setValues] = useState<ClienteFormValues>({
    ...FORMULARIO_CLIENTE_VACIO,
  });
  const [password, setPassword] = useState("");
  const [registrado, setRegistrado] = useState<Cliente | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [guardando, setGuardando] = useState(false);

  function handleChange(field: keyof ClienteFormValues, value: string) {
    setValues((current) => ({ ...current, [field]: value }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setGuardando(true);

    const request: ClienteRegistroRequest = {
      tipoDocumento: values.tipoDocumento,
      numeroDocumento: values.numeroDocumento.trim().toUpperCase(),
      nombres: values.nombres.trim(),
      apellidos: values.apellidos.trim(),
      fechaNacimiento: values.fechaNacimiento,
      email: values.email.trim(),
      telefono: values.telefono.trim() || null,
      licenciaConducir: values.licenciaConducir.trim().toUpperCase() || null,
      vencimientoLicencia: values.vencimientoLicencia || null,
      password,
    };

    try {
      let resultado: Cliente;
      if (modalidad === "autoregistro") {
        resultado = await registrarCliente(request, { modalidad });
      } else {
        if (!token) {
          throw new Error("Se requiere una sesión para registrar clientes.");
        }
        resultado = await registrarCliente(request, { modalidad, token });
      }

      if (onPublicSuccess) {
        onPublicSuccess(resultado);
      } else {
        setRegistrado(resultado);
      }
    } catch (cause) {
      if (
        cause instanceof ApiRequestError &&
        cause.status === 401 &&
        typeof onAuthExpired === "function"
      ) {
        onAuthExpired();
        return;
      }
      setError(
        cause instanceof Error
          ? cause.message
          : "No se pudo registrar el cliente. Inténtalo nuevamente.",
      );
    } finally {
      setGuardando(false);
    }
  }

  if (registrado) {
    return (
      <div className="space-y-5">
        <div className="flex items-start gap-3 rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-emerald-800">
          <CheckCircle2 className="mt-0.5 size-5 shrink-0" aria-hidden="true" />
          <div>
            <p className="font-semibold">Cliente registrado correctamente</p>
            <p className="mt-1 text-sm text-emerald-700">
              La ficha ya está disponible para el equipo Hertz.
            </p>
          </div>
        </div>
        <ClienteResumen cliente={registrado} />
        <Button
          type="button"
          variant="outline"
          className="h-11 rounded-xl"
          onClick={() => {
            setValues({ ...FORMULARIO_CLIENTE_VACIO });
            setRegistrado(null);
          }}
        >
          Registrar otro cliente
        </Button>
      </div>
    );
  }

  return (
    <Card className="rounded-2xl border-0 bg-white shadow-sm ring-1 ring-zinc-200/80">
      <CardContent className="p-5 sm:p-7">
        <div className="mb-6 flex items-start gap-3">
          <div className="grid size-11 shrink-0 place-items-center rounded-xl bg-[#fff4b8] text-zinc-900">
            <UserPlus className="size-5" aria-hidden="true" />
          </div>
          <div>
            <h2 className="text-lg font-semibold text-zinc-950">
              Datos del cliente
            </h2>
            <p className="mt-1 text-sm text-zinc-500">
              Completa la información para crear una ficha de cliente.
            </p>
          </div>
        </div>

        <form className="space-y-6" onSubmit={handleSubmit}>
          <div className="space-y-2">
            <label
              htmlFor="password"
              className="text-sm font-medium text-zinc-700"
            >
              Crear contraseña para iniciar sesión
            </label>
            <Input
              id="password"
              name="password"
              type="password"
              placeholder="Mínimo 8 caracteres"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
              minLength={8}
              maxLength={72}
              autoComplete="new-password"
              className="h-11 rounded-xl border-zinc-200 bg-white px-3 text-sm focus-visible:border-amber-400 focus-visible:ring-amber-300/40"
            />
            <p className="text-xs text-zinc-400">
              Usarás esta contraseña junto con tu correo para acceder a tu cuenta.
            </p>
          </div>

          <ClienteFormFields
            values={values}
            onChange={handleChange}
            incluirIdentidad
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
                  Registrar cliente
                  <ArrowRight className="size-4" aria-hidden="true" />
                </>
              )}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}
