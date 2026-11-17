export const TIPOS_DOCUMENTO = ["DNI", "CE", "PASAPORTE"] as const;

export type TipoDocumento = (typeof TIPOS_DOCUMENTO)[number];

export interface Cliente {
  idCliente: string;
  tipoDocumento: TipoDocumento;
  numeroDocumento: string;
  nombres: string;
  apellidos: string;
  fechaNacimiento: string;
  email: string;
  telefono: string | null;
  licenciaConducir: string | null;
  vencimientoLicencia: string | null;
  licenciaVigente: boolean;
  historialAlquileres: unknown[];
}

export interface ClienteRegistroRequest {
  tipoDocumento: TipoDocumento;
  numeroDocumento: string;
  nombres: string;
  apellidos: string;
  fechaNacimiento: string;
  email: string;
  telefono: string | null;
  licenciaConducir: string | null;
  vencimientoLicencia: string | null;
  password: string;
}

export interface ClienteActualizacionRequest {
  nombres: string;
  apellidos: string;
  email: string;
  telefono: string | null;
  licenciaConducir: string | null;
  vencimientoLicencia: string | null;
}

export interface ClienteFormValues {
  tipoDocumento: TipoDocumento;
  numeroDocumento: string;
  nombres: string;
  apellidos: string;
  fechaNacimiento: string;
  email: string;
  telefono: string;
  licenciaConducir: string;
  vencimientoLicencia: string;
}

export const FORMULARIO_CLIENTE_VACIO: ClienteFormValues = {
  tipoDocumento: "DNI",
  numeroDocumento: "",
  nombres: "",
  apellidos: "",
  fechaNacimiento: "",
  email: "",
  telefono: "",
  licenciaConducir: "",
  vencimientoLicencia: "",
};
