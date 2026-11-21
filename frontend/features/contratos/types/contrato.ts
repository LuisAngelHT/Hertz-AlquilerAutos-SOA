export type EstadoContrato = "VIGENTE" | "FINALIZADO";

export type Combustible = "VACIO" | "UN_CUARTO" | "MEDIO" | "TRES_CUARTOS" | "LLENO";

export type ZonaVehiculo =
  | "FRONTAL"
  | "POSTERIOR"
  | "LATERAL_IZQUIERDO"
  | "LATERAL_DERECHO"
  | "TECHO"
  | "LUNAS"
  | "LLANTAS"
  | "INTERIOR"
  | "OTRO";

export type GravedadDano = "LEVE" | "MODERADO" | "GRAVE";

/** Vista del dibujo del vehículo sobre la que se marca un daño. */
export type VistaVehiculo = "IZQUIERDA" | "DERECHA" | "FRENTE" | "ATRAS" | "SUPERIOR";

export type TipoDano = "RAYON" | "ABOLLADURA" | "ROTURA" | "MANCHA" | "OTRO";

/** Estado de un elemento del inventario (Antes y Después). */
export type EstadoItem = "OK" | "FALTA" | "DANADO";

/** Un daño o defecto anotado en una revisión del vehículo. */
export interface DanoVehiculo {
  zona: ZonaVehiculo;
  descripcion: string;
  gravedad: GravedadDano;
  /** Solo en la devolución: lo que cuesta repararlo (a cargo del cliente). */
  costoEstimado: number | null;
  /** Dónde se marcó en el dibujo (null si se anotó sin ubicarlo, por ejemplo un daño interior). */
  vista: VistaVehiculo | null;
  /** Posición dentro de la vista, de 0 a 100 (porcentaje del ancho y del alto del dibujo). */
  posX: number | null;
  posY: number | null;
  tipo: TipoDano | null;
}

/** Estado de un elemento del inventario en una revisión. */
export interface ItemRevision {
  item: string;
  estado: EstadoItem;
  /** Solo en la devolución: lo que cuesta reponerlo o repararlo (a cargo del cliente). */
  costoEstimado: number | null;
}

/** Lo que se envía por cada elemento; en la devolución, "nuevo" indica que está peor que al entregar. */
export interface ItemPayload extends ItemRevision {
  nuevo?: boolean;
}

/**
 * Revisión del vehículo. La de ENTREGA deja constancia de cómo sale (y de los daños que ya tenía); la de
 * DEVOLUCIÓN, de cómo vuelve: sus daños son los nuevos y los asume el cliente.
 */
export interface RevisionVehiculo {
  kilometraje: number;
  combustible: Combustible;
  observaciones: string | null;
  /** Solo en la devolución: el cliente aceptó lo registrado. */
  clienteConforme: boolean | null;
  registradoPor: string;
  registradoEn: string;
  danos: DanoVehiculo[];
  /** Puede faltar en revisiones hechas antes del inventario. */
  inventario?: ItemRevision[];
}

/** Lo que se envía al entregar o al recibir el vehículo. */
export interface RevisionPayload {
  kilometraje: number;
  combustible: Combustible;
  danos: DanoVehiculo[];
  inventario: ItemPayload[];
  observaciones: string | null;
}

/** Contrato firmado y escaneado (el archivo en si se pide aparte, al abrirlo). */
export interface DocumentoContrato {
  nombreArchivo: string;
  tipoContenido: string;
  tamanoBytes: number;
  subidoPor: string;
  subidoEn: string;
}

export interface Contrato {
  idContrato: string;
  idReserva: string;
  idCliente: string;
  placa: string;
  sucursalRecojo: string;
  sucursalDevolucion: string;
  fechaInicio: string;
  fechaFinPrevista: string;
  fechaDevolucion: string | null;
  estado: EstadoContrato;
  registradoPor: string;
  creadoEn: string;
  actualizadoEn: string;
  /** null mientras no se haya subido el contrato firmado y escaneado. */
  documento: DocumentoContrato | null;
  /** Revisión al entregar el vehículo; null en contratos anteriores a esta función. */
  entrega: RevisionVehiculo | null;
  /** Revisión al recibirlo; null mientras el contrato siga vigente. */
  devolucion: RevisionVehiculo | null;
  kilometrosRecorridos: number | null;
  /** Suma del costo estimado de los daños nuevos: lo que asume el cliente. null hasta la devolución. */
  totalDanosACargoCliente: number | null;
}

export interface PaginaContratos {
  total: number;
  pagina: number;
  tamano: number;
  totalPaginas: number;
  contratos: Contrato[];
}

export interface FiltroContratos {
  placa?: string;
  idReserva?: string;
  idCliente?: string;
  estado?: EstadoContrato;
  pagina: number;
  tamano: number;
}
