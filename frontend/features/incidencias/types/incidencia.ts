export const TIPOS_INCIDENCIA = [
  { value: "DAÑO", label: "Daño" },
  { value: "MANTENIMIENTO", label: "Mantenimiento" },
  { value: "SINIESTRO", label: "Siniestro" },
] as const;

export type TipoIncidencia = (typeof TIPOS_INCIDENCIA)[number]["value"];
export type EstadoIncidencia = "ABIERTA" | "CERRADA";

export interface Incidencia {
  idIncidencia: string;
  idVehiculo: string;
  sucursal: string;
  idContrato: string | null;
  tipo: TipoIncidencia;
  descripcion: string;
  costoEstimado: number | null;
  estado: EstadoIncidencia;
  registradaPor: string;
  creadoEn: string;
}

export interface PaginaIncidencias {
  total: number;
  pagina: number;
  tamano: number;
  totalPaginas: number;
  incidencias: Incidencia[];
}

export interface NuevaIncidencia {
  idVehiculo: string;
  idContrato: string | null;
  tipo: TipoIncidencia;
  descripcion: string;
  costoEstimado: number | null;
}

export interface RegistroIncidencia {
  idIncidencia: string;
  estado: EstadoIncidencia;
  estadoVehiculo: string;
  idVehiculo: string;
}
