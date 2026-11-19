export type EstadoContrato = "VIGENTE" | "FINALIZADO";

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
