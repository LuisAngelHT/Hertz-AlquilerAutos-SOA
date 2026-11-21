import type {
  DanoExternoFicha,
  DanoInternoFicha,
  FichaTaller,
  GuardarFicha,
  SistemaVehiculo,
} from "@/features/incidencias/types/incidencia";
import type { GravedadDano, TipoDano, VistaVehiculo } from "@/features/contratos/types/contrato";

export const SISTEMAS: { value: SistemaVehiculo; label: string }[] = [
  { value: "MOTOR", label: "Motor" },
  { value: "TRANSMISION", label: "Transmisión" },
  { value: "FRENOS", label: "Frenos" },
  { value: "SUSPENSION", label: "Suspensión" },
  { value: "DIRECCION", label: "Dirección" },
  { value: "ELECTRICO", label: "Sistema eléctrico" },
  { value: "ENFRIAMIENTO", label: "Enfriamiento (radiador)" },
  { value: "AIRE_ACONDICIONADO", label: "Aire acondicionado" },
  { value: "ESCAPE", label: "Escape" },
  { value: "OTRO", label: "Otro" },
];

export function etiquetaSistema(valor: string): string {
  return SISTEMAS.find((s) => s.value === valor)?.label ?? valor;
}

export interface InternoForm {
  clave: number;
  sistema: SistemaVehiculo;
  gravedad: GravedadDano;
  descripcion: string;
  costo: string;
}

export interface ExternoForm {
  clave: number;
  vista: VistaVehiculo;
  x: number;
  y: number;
  tipo: TipoDano;
  gravedad: GravedadDano;
  descripcion: string;
  costo: string;
}

export interface FichaForm {
  diagnostico: string;
  dias: string;
  repuestos: string;
  manoObra: string;
  internos: InternoForm[];
  externos: ExternoForm[];
}

let contador = 0;
const clave = () => ++contador;

export function internoNuevo(): InternoForm {
  return { clave: clave(), sistema: "MOTOR", gravedad: "MODERADO", descripcion: "", costo: "" };
}

export function externoNuevo(vista: VistaVehiculo, x: number, y: number): ExternoForm {
  return {
    clave: clave(),
    vista,
    x: Math.round(x * 100) / 100,
    y: Math.round(y * 100) / 100,
    tipo: "ABOLLADURA",
    gravedad: "MODERADO",
    descripcion: "",
    costo: "",
  };
}

const texto = (n: number | null | undefined) => (n == null ? "" : String(n));

export function formDesdeFicha(ficha: FichaTaller | null): FichaForm {
  return {
    diagnostico: ficha?.diagnostico ?? "",
    dias: texto(ficha?.diasEstimados),
    repuestos: texto(ficha?.costoRepuestos),
    manoObra: texto(ficha?.costoManoObra),
    internos: (ficha?.internos ?? []).map((d) => ({
      clave: clave(),
      sistema: d.sistema,
      gravedad: d.gravedad,
      descripcion: d.descripcion,
      costo: texto(d.costoEstimado),
    })),
    externos: (ficha?.externos ?? []).map((d) => ({
      clave: clave(),
      vista: d.vista,
      x: d.posX,
      y: d.posY,
      tipo: d.tipo,
      gravedad: d.gravedad,
      descripcion: d.descripcion,
      costo: texto(d.costoEstimado),
    })),
  };
}

function numero(t: string): number | null {
  if (t.trim() === "") return null;
  const n = Number(t);
  return Number.isFinite(n) ? Math.round(n * 100) / 100 : null;
}

/** Texto del primer problema de la ficha, o null si se puede guardar. */
export function errorFicha(f: FichaForm): string | null {
  if (f.dias.trim() !== "") {
    const dias = Number(f.dias);
    if (!Number.isInteger(dias) || dias < 0 || dias > 365) return "Los días estimados deben ser un número entero entre 0 y 365.";
  }
  for (const [nombre, t] of [["repuestos", f.repuestos], ["mano de obra", f.manoObra]] as const) {
    if (t.trim() !== "" && (!Number.isFinite(Number(t)) || Number(t) < 0)) return `El costo de ${nombre} debe ser un número mayor o igual a 0.`;
  }
  for (const d of [...f.internos, ...f.externos]) {
    if (!d.descripcion.trim()) return "Describe cada daño anotado, o quítalo de la lista.";
    if (d.costo.trim() !== "" && (!Number.isFinite(Number(d.costo)) || Number(d.costo) < 0)) return "Los costos de los daños deben ser números mayores o iguales a 0.";
  }
  return null;
}

export function fichaAPayload(f: FichaForm): GuardarFicha {
  return {
    diagnostico: f.diagnostico.trim() || null,
    diasEstimados: f.dias.trim() === "" ? null : Number(f.dias),
    costoRepuestos: numero(f.repuestos),
    costoManoObra: numero(f.manoObra),
    internos: f.internos.map((d): DanoInternoFicha => ({
      sistema: d.sistema,
      descripcion: d.descripcion.trim(),
      gravedad: d.gravedad,
      costoEstimado: numero(d.costo),
    })),
    externos: f.externos.map((d): DanoExternoFicha => ({
      vista: d.vista,
      posX: d.x,
      posY: d.y,
      tipo: d.tipo,
      descripcion: d.descripcion.trim(),
      gravedad: d.gravedad,
      costoEstimado: numero(d.costo),
    })),
  };
}

export function totalDanos(f: FichaForm): number {
  return [...f.internos, ...f.externos].reduce((s, d) => s + (numero(d.costo) ?? 0), 0);
}
