import type {
  Combustible,
  DanoVehiculo,
  EstadoItem,
  GravedadDano,
  ItemPayload,
  RevisionPayload,
  RevisionVehiculo,
  TipoDano,
  VistaVehiculo,
  ZonaVehiculo,
} from "@/features/contratos/types/contrato";

// Revisión del vehículo al entregarlo y al recibirlo: opciones, etiquetas, estado del formulario y validación.

export const COMBUSTIBLES: { value: Combustible; label: string }[] = [
  { value: "VACIO", label: "Vacío" },
  { value: "UN_CUARTO", label: "1/4" },
  { value: "MEDIO", label: "1/2" },
  { value: "TRES_CUARTOS", label: "3/4" },
  { value: "LLENO", label: "Lleno" },
];

/** Zonas que no se pueden marcar en el dibujo (interior, lunas, llantas...) y las que salen de cada vista. */
export const ZONAS: { value: ZonaVehiculo; label: string }[] = [
  { value: "FRONTAL", label: "Parte delantera" },
  { value: "POSTERIOR", label: "Parte trasera" },
  { value: "LATERAL_IZQUIERDO", label: "Lado izquierdo" },
  { value: "LATERAL_DERECHO", label: "Lado derecho" },
  { value: "TECHO", label: "Techo" },
  { value: "LUNAS", label: "Lunas y espejos" },
  { value: "LLANTAS", label: "Llantas y aros" },
  { value: "INTERIOR", label: "Interior" },
  { value: "OTRO", label: "Otra zona" },
];

/** Zonas para un daño que NO se marca en el dibujo. */
export const ZONAS_SIN_DIBUJO = ZONAS.filter((z) => ["LUNAS", "LLANTAS", "INTERIOR", "OTRO"].includes(z.value));

export const VISTAS: { value: VistaVehiculo; label: string }[] = [
  { value: "IZQUIERDA", label: "Lado izquierdo" },
  { value: "DERECHA", label: "Lado derecho" },
  { value: "FRENTE", label: "Frente" },
  { value: "ATRAS", label: "Parte trasera" },
  { value: "SUPERIOR", label: "Vista superior" },
];

export const TIPOS_DANO: { value: TipoDano; label: string }[] = [
  { value: "RAYON", label: "Rayón" },
  { value: "ABOLLADURA", label: "Abolladura" },
  { value: "ROTURA", label: "Rotura o quiebre" },
  { value: "MANCHA", label: "Mancha o desgaste" },
  { value: "OTRO", label: "Otro" },
];

export const GRAVEDADES: { value: GravedadDano; label: string }[] = [
  { value: "LEVE", label: "Leve" },
  { value: "MODERADO", label: "Moderado" },
  { value: "GRAVE", label: "Grave" },
];

/** La zona que corresponde a cada vista del dibujo. */
export function zonaDeVista(vista: VistaVehiculo): ZonaVehiculo {
  switch (vista) {
    case "IZQUIERDA":
      return "LATERAL_IZQUIERDO";
    case "DERECHA":
      return "LATERAL_DERECHO";
    case "FRENTE":
      return "FRONTAL";
    case "ATRAS":
      return "POSTERIOR";
    default:
      return "TECHO";
  }
}

export function etiquetaCombustible(valor: string): string {
  const etiqueta = COMBUSTIBLES.find((c) => c.value === valor)?.label ?? valor;
  return etiqueta === "Lleno" || etiqueta === "Vacío" ? `Tanque ${etiqueta.toLowerCase()}` : `Tanque a ${etiqueta}`;
}

export function etiquetaZona(valor: string): string {
  return ZONAS.find((z) => z.value === valor)?.label ?? valor;
}

export function etiquetaVista(valor: string): string {
  return VISTAS.find((v) => v.value === valor)?.label ?? valor;
}

export function etiquetaTipoDano(valor: string): string {
  return TIPOS_DANO.find((t) => t.value === valor)?.label ?? valor;
}

export function etiquetaGravedad(valor: string): string {
  return GRAVEDADES.find((g) => g.value === valor)?.label ?? valor;
}

/** Nivel de combustible de 0 (vacío) a 4 (lleno), para comparar lo entregado con lo devuelto. */
export function cuartos(valor: string): number {
  return Math.max(0, COMBUSTIBLES.findIndex((c) => c.value === valor));
}

export function formatoSoles(monto: number): string {
  return new Intl.NumberFormat("es-PE", { style: "currency", currency: "PEN" }).format(monto);
}

export function formatoKm(km: number): string {
  return `${new Intl.NumberFormat("es-PE").format(km)} km`;
}

// ---------------------------------------------------------------- inventario (Antes y Después)

export const ESTADOS_ITEM: { value: EstadoItem; label: string }[] = [
  { value: "OK", label: "Bien" },
  { value: "FALTA", label: "Falta" },
  { value: "DANADO", label: "Dañado" },
];

export const GRUPOS_INVENTARIO: { grupo: string; items: { codigo: string; etiqueta: string }[] }[] = [
  {
    grupo: "Exteriores",
    items: [
      { codigo: "LUCES_PRINCIPALES", etiqueta: "Luces principales" },
      { codigo: "LUCES_FRENO_DIRECCIONALES", etiqueta: "Luces de freno y direccionales" },
      { codigo: "LIMPIAPARABRISAS", etiqueta: "Limpiaparabrisas" },
      { codigo: "ESPEJOS_LATERALES", etiqueta: "Espejos laterales" },
      { codigo: "PARABRISAS_Y_LUNAS", etiqueta: "Parabrisas y lunas" },
      { codigo: "LLANTAS_Y_TAPONES", etiqueta: "Llantas y tapones" },
      { codigo: "TAPON_GASOLINA", etiqueta: "Tapón de gasolina" },
      { codigo: "PLACAS", etiqueta: "Placas delantera y trasera" },
    ],
  },
  {
    grupo: "Interiores",
    items: [
      { codigo: "RADIO", etiqueta: "Radio" },
      { codigo: "ESPEJO_RETROVISOR", etiqueta: "Espejo retrovisor" },
      { codigo: "CINTURONES", etiqueta: "Cinturones" },
      { codigo: "TAPETES", etiqueta: "Tapetes" },
      { codigo: "ASIENTOS_Y_FUNDAS", etiqueta: "Asientos y fundas" },
      { codigo: "MANIJAS_Y_SEGUROS", etiqueta: "Manijas y seguros" },
      { codigo: "AIRE_ACONDICIONADO", etiqueta: "Aire acondicionado" },
    ],
  },
  {
    grupo: "Accesorios",
    items: [
      { codigo: "GATA", etiqueta: "Gata" },
      { codigo: "LLAVE_DE_RUEDAS", etiqueta: "Llave de ruedas" },
      { codigo: "LLANTA_DE_AUXILIO", etiqueta: "Llanta de auxilio" },
      { codigo: "TRIANGULO", etiqueta: "Triángulo" },
      { codigo: "EXTINTOR", etiqueta: "Extintor" },
      { codigo: "BOTIQUIN", etiqueta: "Botiquín" },
    ],
  },
  {
    grupo: "Documentos",
    items: [
      { codigo: "SOAT", etiqueta: "SOAT" },
      { codigo: "REVISION_TECNICA", etiqueta: "Revisión técnica" },
      { codigo: "TARJETA_DE_PROPIEDAD", etiqueta: "Tarjeta de propiedad" },
    ],
  },
];

export const CODIGOS_INVENTARIO: string[] = GRUPOS_INVENTARIO.flatMap((g) => g.items.map((i) => i.codigo));

export function etiquetaItem(codigo: string): string {
  for (const grupo of GRUPOS_INVENTARIO) {
    const item = grupo.items.find((i) => i.codigo === codigo);
    if (item) return item.etiqueta;
  }
  return codigo;
}

export function etiquetaEstadoItem(estado: string): string {
  return ESTADOS_ITEM.find((e) => e.value === estado)?.label ?? estado;
}

function gravedadItem(estado: EstadoItem): number {
  return estado === "OK" ? 0 : estado === "FALTA" ? 1 : 2;
}

/** Estado de cada elemento tal como quedó en una revisión (los que no figuran, como "Bien"). */
export function estadosDeRevision(revision: RevisionVehiculo | null): Record<string, EstadoItem> {
  const mapa: Record<string, EstadoItem> = {};
  for (const codigo of CODIGOS_INVENTARIO) mapa[codigo] = "OK";
  for (const item of revision?.inventario ?? []) mapa[item.item] = item.estado;
  return mapa;
}

// ---------------------------------------------------------------- estado del formulario

export interface DanoForm {
  /** Solo sirve de clave en la lista; no se envía. */
  clave: number;
  /** Vista del dibujo y posición (0 a 100) si se marcó ahí; null si se anotó sin ubicarlo (interior, lunas...). */
  vista: VistaVehiculo | null;
  x: number | null;
  y: number | null;
  zona: ZonaVehiculo;
  tipo: TipoDano;
  descripcion: string;
  gravedad: GravedadDano;
  /** Texto del costo estimado (solo en la devolución); vacío = sin estimar. */
  costo: string;
}

export interface RevisionForm {
  kilometraje: string;
  combustible: Combustible | "";
  danos: DanoForm[];
  /** Estado de cada elemento del inventario (por código). */
  inventario: Record<string, EstadoItem>;
  /** Costo estimado (texto) de los elementos que faltan o vuelven dañados; solo en la devolución. */
  costosInventario: Record<string, string>;
  observaciones: string;
}

export const MAX_DANOS = 30;

function inventarioCompleto(estados?: Record<string, EstadoItem>): Record<string, EstadoItem> {
  const mapa: Record<string, EstadoItem> = {};
  for (const codigo of CODIGOS_INVENTARIO) mapa[codigo] = estados?.[codigo] ?? "OK";
  return mapa;
}

/** Formulario nuevo. En la devolución se parte del inventario con que salió, para marcar solo lo que cambió. */
export function revisionVacia(entrega?: RevisionVehiculo | null): RevisionForm {
  return {
    kilometraje: "",
    combustible: "",
    danos: [],
    inventario: inventarioCompleto(entrega ? estadosDeRevision(entrega) : undefined),
    costosInventario: {},
    observaciones: "",
  };
}

let contadorClaves = 0;

function claveNueva(): number {
  contadorClaves += 1;
  return contadorClaves;
}

/** Daño marcado con un clic sobre el dibujo. */
export function danoEnDibujo(vista: VistaVehiculo, x: number, y: number): DanoForm {
  return {
    clave: claveNueva(),
    vista,
    x: Math.round(x * 100) / 100,
    y: Math.round(y * 100) / 100,
    zona: zonaDeVista(vista),
    tipo: "RAYON",
    descripcion: "",
    gravedad: "LEVE",
    costo: "",
  };
}

/** Daño que no se puede marcar en el dibujo (interior, lunas, llantas...). */
export function danoSinUbicar(): DanoForm {
  return {
    clave: claveNueva(),
    vista: null,
    x: null,
    y: null,
    zona: "INTERIOR",
    tipo: "OTRO",
    descripcion: "",
    gravedad: "LEVE",
    costo: "",
  };
}

/** Descripción que se guarda: la escrita, o una por defecto con el tipo y el lugar. */
export function descripcionDano(dano: DanoForm): string {
  const escrita = dano.descripcion.trim();
  if (escrita) return escrita;
  const lugar = dano.vista ? etiquetaVista(dano.vista).toLowerCase() : etiquetaZona(dano.zona).toLowerCase();
  return `${etiquetaTipoDano(dano.tipo)} en ${lugar}`;
}

/** ¿El elemento está peor que cuando se entregó el vehículo? (solo tiene sentido en la devolución). */
export function itemEmpeoro(codigo: string, revision: RevisionForm, entrega: RevisionVehiculo | null): boolean {
  const antes = estadosDeRevision(entrega)[codigo] ?? "OK";
  return gravedadItem(revision.inventario[codigo] ?? "OK") > gravedadItem(antes);
}

/** Texto del primer problema del formulario, o null si está listo para enviarse. */
export function errorRevision(
  revision: RevisionForm,
  opciones: { esDevolucion: boolean; kmMinimo?: number | null },
): string | null {
  const km = Number(revision.kilometraje);
  if (revision.kilometraje.trim() === "" || !Number.isInteger(km) || km < 0 || km > 2_000_000) {
    return "Escribe el kilometraje actual del vehículo (un número entero).";
  }
  if (opciones.kmMinimo != null && km < opciones.kmMinimo) {
    return `El kilometraje no puede ser menor al de la entrega (${formatoKm(opciones.kmMinimo)}).`;
  }
  if (!revision.combustible) {
    return "Indica el nivel de combustible.";
  }
  if (opciones.esDevolucion) {
    const costos = [...revision.danos.map((d) => d.costo), ...Object.values(revision.costosInventario)];
    for (const texto of costos) {
      if (texto.trim() === "") continue;
      const costo = Number(texto);
      if (!Number.isFinite(costo) || costo < 0) {
        return "Los costos estimados deben ser números mayores o iguales a 0.";
      }
    }
  }
  return null;
}

/** Lo que se envía al servidor. En la entrega nada lleva costo (ya estaba así: no se cobra). */
export function revisionAPayload(
  revision: RevisionForm,
  esDevolucion: boolean,
  entrega: RevisionVehiculo | null = null,
): RevisionPayload {
  const danos: DanoVehiculo[] = revision.danos.map((d) => ({
    zona: d.zona,
    descripcion: descripcionDano(d),
    gravedad: d.gravedad,
    costoEstimado: esDevolucion && d.costo.trim() !== "" ? Math.round(Number(d.costo) * 100) / 100 : null,
    vista: d.vista,
    posX: d.vista ? d.x : null,
    posY: d.vista ? d.y : null,
    tipo: d.tipo,
  }));
  const inventario: ItemPayload[] = CODIGOS_INVENTARIO.map((codigo) => {
    const texto = revision.costosInventario[codigo]?.trim() ?? "";
    return {
      item: codigo,
      estado: revision.inventario[codigo] ?? "OK",
      costoEstimado: esDevolucion && texto !== "" ? Math.round(Number(texto) * 100) / 100 : null,
      ...(esDevolucion ? { nuevo: itemEmpeoro(codigo, revision, entrega) } : {}),
    };
  });
  return {
    kilometraje: Number(revision.kilometraje),
    combustible: revision.combustible as Combustible,
    danos,
    inventario,
    observaciones: revision.observaciones.trim() || null,
  };
}

/** Suma de los costos estimados (daños nuevos y elementos que faltan o vuelven dañados): lo que asume el cliente. */
export function totalACargo(revision: RevisionForm): number {
  const suma = (textos: string[]) =>
    textos.reduce((total, t) => {
      const costo = Number(t);
      return total + (t.trim() !== "" && Number.isFinite(costo) && costo > 0 ? costo : 0);
    }, 0);
  return suma(revision.danos.map((d) => d.costo)) + suma(Object.values(revision.costosInventario));
}
