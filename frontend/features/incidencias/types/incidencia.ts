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
  /** Cuándo se espera que el vehículo salga del taller (null si el mecánico aún no estimó los días). */
  fechaSalidaPrevista: string | null;
  /** El mecánico ya empezó a llenar la ficha del taller. */
  tieneFicha: boolean;
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

// ---------------------------------------------------------------- ficha técnica del taller

export type SistemaVehiculo =
  | "MOTOR"
  | "TRANSMISION"
  | "FRENOS"
  | "SUSPENSION"
  | "DIRECCION"
  | "ELECTRICO"
  | "ENFRIAMIENTO"
  | "AIRE_ACONDICIONADO"
  | "ESCAPE"
  | "OTRO";

export interface DanoInternoFicha {
  sistema: SistemaVehiculo;
  descripcion: string;
  gravedad: "LEVE" | "MODERADO" | "GRAVE";
  costoEstimado: number | null;
}

export interface DanoExternoFicha {
  vista: "IZQUIERDA" | "DERECHA" | "FRENTE" | "ATRAS" | "SUPERIOR";
  posX: number;
  posY: number;
  tipo: "RAYON" | "ABOLLADURA" | "ROTURA" | "MANCHA" | "OTRO";
  descripcion: string;
  gravedad: "LEVE" | "MODERADO" | "GRAVE";
  costoEstimado: number | null;
}

export interface SeguimientoFicha {
  nota: string;
  registradoPor: string;
  registradoEn: string;
}

/** Lo que el mecánico registra mientras el vehículo está en mantenimiento. */
export interface FichaTaller {
  idIncidencia: string;
  estadoIncidencia: EstadoIncidencia;
  /** Día en que el vehículo entró a taller (cuando se registró la incidencia). */
  ingresoATaller: string;
  diagnostico: string | null;
  diasEstimados: number | null;
  fechaSalidaPrevista: string | null;
  costoRepuestos: number | null;
  costoManoObra: number | null;
  trabajoRealizado: string | null;
  costoFinal: number | null;
  actualizadoPor: string | null;
  actualizadoEn: string | null;
  internos: DanoInternoFicha[];
  externos: DanoExternoFicha[];
  seguimiento: SeguimientoFicha[];
}

export interface GuardarFicha {
  diagnostico: string | null;
  diasEstimados: number | null;
  costoRepuestos: number | null;
  costoManoObra: number | null;
  internos: DanoInternoFicha[];
  externos: DanoExternoFicha[];
}

export interface CierreIncidencia {
  trabajoRealizado: string;
  costoFinal: number;
}
