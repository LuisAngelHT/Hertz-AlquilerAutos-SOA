import type { ReactNode } from "react";
import { Input } from "@/components/ui/input";
import {
  TIPOS_DOCUMENTO,
  type ClienteFormValues,
} from "@/features/clientes/types/cliente";

interface ClienteFormFieldsProps {
  values: ClienteFormValues;
  onChange: (field: keyof ClienteFormValues, value: string) => void;
  incluirIdentidad: boolean;
}

const inputClassName =
  "h-11 rounded-xl border-zinc-200 bg-white px-3 text-sm focus-visible:border-amber-400 focus-visible:ring-amber-300/40";

function Field({
  id,
  label,
  children,
}: {
  id: string;
  label: string;
  children: ReactNode;
}) {
  return (
    <div className="space-y-2">
      <label htmlFor={id} className="text-sm font-medium text-zinc-700">
        {label}
      </label>
      {children}
    </div>
  );
}

export function ClienteFormFields({
  values,
  onChange,
  incluirIdentidad,
}: ClienteFormFieldsProps) {
  const documentPattern =
    values.tipoDocumento === "DNI"
      ? "\\d{8}"
      : values.tipoDocumento === "CE"
        ? "[A-Za-z0-9]{9,12}"
        : "[A-Za-z0-9]{6,12}";
  const documentHint =
    values.tipoDocumento === "DNI"
      ? "8 dígitos"
      : values.tipoDocumento === "CE"
        ? "9 a 12 letras o números"
        : "6 a 12 letras o números";

  return (
    <div className="grid gap-5 sm:grid-cols-2">
      {incluirIdentidad && (
        <>
          <Field id="tipoDocumento" label="Tipo de documento">
            <select
              id="tipoDocumento"
              name="tipoDocumento"
              value={values.tipoDocumento}
              onChange={(event) =>
                onChange("tipoDocumento", event.target.value)
              }
              className={`${inputClassName} w-full`}
            >
              {TIPOS_DOCUMENTO.map((tipo) => (
                <option key={tipo} value={tipo}>
                  {tipo}
                </option>
              ))}
            </select>
          </Field>
          <Field id="numeroDocumento" label="Número de documento">
            <Input
              id="numeroDocumento"
              name="numeroDocumento"
              value={values.numeroDocumento}
              onChange={(event) =>
                onChange("numeroDocumento", event.target.value.toUpperCase())
              }
              required
              maxLength={20}
              pattern={documentPattern}
              title={`El formato válido para ${values.tipoDocumento} es: ${documentHint}.`}
              placeholder={
                values.tipoDocumento === "DNI" ? "12345678" : "Documento"
              }
              className={inputClassName}
            />
            <p className="text-xs text-zinc-400">{documentHint}</p>
          </Field>
        </>
      )}

      <Field id="nombres" label="Nombres">
        <Input
          id="nombres"
          name="nombres"
          value={values.nombres}
          onChange={(event) => onChange("nombres", event.target.value)}
          required
          maxLength={100}
          autoComplete="given-name"
          className={inputClassName}
        />
      </Field>
      <Field id="apellidos" label="Apellidos">
        <Input
          id="apellidos"
          name="apellidos"
          value={values.apellidos}
          onChange={(event) => onChange("apellidos", event.target.value)}
          required
          maxLength={100}
          autoComplete="family-name"
          className={inputClassName}
        />
      </Field>
      {incluirIdentidad && (
        <Field id="fechaNacimiento" label="Fecha de nacimiento">
          <Input
            id="fechaNacimiento"
            name="fechaNacimiento"
            type="date"
            value={values.fechaNacimiento}
            onChange={(event) => onChange("fechaNacimiento", event.target.value)}
            required
            className={inputClassName}
          />
        </Field>
      )}
      <Field id="email" label="Correo electrónico">
        <Input
          id="email"
          name="email"
          type="email"
          value={values.email}
          onChange={(event) => onChange("email", event.target.value)}
          required
          maxLength={120}
          autoComplete="email"
          placeholder="cliente@correo.com"
          className={inputClassName}
        />
      </Field>
      <Field id="telefono" label="Teléfono (opcional)">
        <Input
          id="telefono"
          name="telefono"
          type="tel"
          value={values.telefono}
          onChange={(event) => onChange("telefono", event.target.value)}
          maxLength={20}
          autoComplete="tel"
          className={inputClassName}
        />
      </Field>
      <Field id="licenciaConducir" label="Licencia de conducir (opcional)">
        <Input
          id="licenciaConducir"
          name="licenciaConducir"
          value={values.licenciaConducir}
          onChange={(event) =>
            onChange("licenciaConducir", event.target.value.toUpperCase())
          }
          maxLength={20}
          autoComplete="off"
          className={inputClassName}
        />
      </Field>
      <Field id="vencimientoLicencia" label="Vencimiento de licencia">
        <Input
          id="vencimientoLicencia"
          name="vencimientoLicencia"
          type="date"
          value={values.vencimientoLicencia}
          onChange={(event) =>
            onChange("vencimientoLicencia", event.target.value)
          }
          className={inputClassName}
        />
      </Field>
    </div>
  );
}
