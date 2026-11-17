"use client";

import { useState, type FormEvent } from "react";
import { Search, UserRoundSearch } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { ApiRequestError } from "@/lib/api-client";
import { ClienteEdicionForm } from "@/features/clientes/components/cliente-edicion-form";
import { ClienteResumen } from "@/features/clientes/components/cliente-resumen";
import { buscarCliente } from "@/features/clientes/lib/cliente-api";
import {
  TIPOS_DOCUMENTO,
  type Cliente,
  type TipoDocumento,
} from "@/features/clientes/types/cliente";

interface ClienteBusquedaProps {
  token: string;
  onAuthExpired: () => void;
}

export function ClienteBusqueda({
  token,
  onAuthExpired,
}: ClienteBusquedaProps) {
  const [tipoDocumento, setTipoDocumento] =
    useState<TipoDocumento>("DNI");
  const [numeroDocumento, setNumeroDocumento] = useState("");
  const [cliente, setCliente] = useState<Cliente | null>(null);
  const [editando, setEditando] = useState(false);
  const [buscando, setBuscando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setCliente(null);
    setEditando(false);
    setBuscando(true);

    try {
      const resultado = await buscarCliente(
        token,
        tipoDocumento,
        numeroDocumento,
      );
      setCliente(resultado);
    } catch (cause) {
      if (cause instanceof ApiRequestError && cause.status === 401) {
        onAuthExpired();
        return;
      }
      setError(
        cause instanceof Error
          ? cause.message
          : "No se pudo buscar el cliente.",
      );
    } finally {
      setBuscando(false);
    }
  }

  function handleSaved(updated: Cliente) {
    setCliente(updated);
    setEditando(false);
  }

  return (
    <div className="space-y-5">
      <Card className="rounded-2xl border-0 bg-white shadow-sm ring-1 ring-zinc-200/80">
        <CardContent className="p-5 sm:p-7">
          <div className="mb-6 flex items-start gap-3">
            <div className="grid size-11 shrink-0 place-items-center rounded-xl bg-[#fff4b8] text-zinc-900">
              <UserRoundSearch className="size-5" aria-hidden="true" />
            </div>
            <div>
              <h2 className="text-lg font-semibold text-zinc-950">
                Buscar por documento
              </h2>
              <p className="mt-1 text-sm text-zinc-500">
                Consulta la ficha de un cliente para revisar o actualizar sus
                datos.
              </p>
            </div>
          </div>

          <form
            className="grid gap-4 sm:grid-cols-[minmax(150px,0.7fr)_1.3fr_auto] sm:items-end"
            onSubmit={handleSubmit}
          >
            <div className="space-y-2">
              <label
                htmlFor="tipoBusqueda"
                className="text-sm font-medium text-zinc-700"
              >
                Tipo
              </label>
              <select
                id="tipoBusqueda"
                value={tipoDocumento}
                onChange={(event) =>
                  setTipoDocumento(event.target.value as TipoDocumento)
                }
                className="h-11 w-full rounded-xl border border-zinc-200 bg-white px-3 text-sm outline-none focus-visible:border-amber-400 focus-visible:ring-3 focus-visible:ring-amber-300/40"
              >
                {TIPOS_DOCUMENTO.map((tipo) => (
                  <option key={tipo} value={tipo}>
                    {tipo}
                  </option>
                ))}
              </select>
            </div>
            <div className="space-y-2">
              <label
                htmlFor="numeroBusqueda"
                className="text-sm font-medium text-zinc-700"
              >
                Número de documento
              </label>
              <Input
                id="numeroBusqueda"
                value={numeroDocumento}
                onChange={(event) =>
                  setNumeroDocumento(event.target.value.toUpperCase())
                }
                required
                maxLength={20}
                placeholder={tipoDocumento === "DNI" ? "12345678" : "Documento"}
                className="h-11 rounded-xl border-zinc-200 bg-white px-3 text-sm"
              />
            </div>
            <Button
              type="submit"
              disabled={buscando}
              className="h-11 rounded-xl bg-[#ffd400] px-5 font-semibold text-zinc-950 hover:bg-[#f2c900]"
            >
              <Search className="size-4" aria-hidden="true" />
              {buscando ? "Buscando..." : "Buscar"}
            </Button>
          </form>

          {error && (
            <div
              role="alert"
              className="mt-5 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"
            >
              {error}
            </div>
          )}
        </CardContent>
      </Card>

      {cliente && !editando && (
        <div className="space-y-4">
          <ClienteResumen cliente={cliente} />
          <Button
            type="button"
            variant="outline"
            className="h-10 rounded-xl"
            onClick={() => setEditando(true)}
          >
            Editar datos
          </Button>
        </div>
      )}

      {cliente && editando && (
        <ClienteEdicionForm
          key={cliente.idCliente}
          cliente={cliente}
          token={token}
          onSaved={handleSaved}
          onCancel={() => setEditando(false)}
          onAuthExpired={onAuthExpired}
        />
      )}
    </div>
  );
}
