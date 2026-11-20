/**
 * Valores fijos de la flota. El backend no tiene un endpoint que liste sucursales ni
 * categorías, así que se declaran aquí. Si se agrega una sucursal o categoría nueva en
 * la base de datos (flota.vehiculo), hay que agregarla también aquí.
 */

export const SUCURSALES = [
  { value: "LIMA_AEROPUERTO", label: "Lima · Aeropuerto Jorge Chávez" },
  { value: "LIMA_MIRAFLORES", label: "Lima · Miraflores" },
  { value: "CUSCO_AEROPUERTO", label: "Cusco · Aeropuerto Velasco Astete" },
  { value: "AREQUIPA_AEROPUERTO", label: "Arequipa · Aeropuerto Rodríguez Ballón" },
] as const;

export const CATEGORIAS = [
  { value: "ECONOMICO", label: "Económico" },
  { value: "SUV", label: "SUV" },
  { value: "CAMIONETA", label: "Camioneta" },
  { value: "PREMIUM", label: "Premium" },
] as const;

/**
 * Motivos predefinidos del cambio de estado, según el estado actual del vehículo. Así el
 * historial queda con causas comparables en vez de texto libre; "Otro" permite escribir uno.
 */
export const MOTIVOS_CAMBIO: Record<"DISPONIBLE" | "ALQUILADO" | "MANTENIMIENTO" | "FUERA_DE_SERVICIO", string[]> = {
  DISPONIBLE: [
    "Entrega al cliente en mostrador",
    "Reserva de cliente web",
    "Mantenimiento programado",
    "Daño o incidente",
    "Revisión técnica o documentaria",
    "Baja: venta del vehículo",
    "Baja: siniestro (pérdida total)",
    "Baja: fin de vida útil",
  ],
  ALQUILADO: [
    "Devolución del cliente",
    "Devuelto con daños",
    "Devuelto con retraso",
    "Mantenimiento programado",
  ],
  MANTENIMIENTO: [
    "Mantenimiento terminado",
    "Reparación terminada",
    "Revisión aprobada",
    "Baja: venta del vehículo",
    "Baja: siniestro (pérdida total)",
    "Baja: fin de vida útil",
  ],
  FUERA_DE_SERVICIO: [
    "Reactivación tras revisión",
    "Baja registrada por error",
  ],
};

export const MOTIVO_OTRO = "__otro__";

export function etiquetaSucursal(valor: string): string {
  return SUCURSALES.find((s) => s.value === valor)?.label ?? valor;
}

export function etiquetaCategoria(valor: string): string {
  return CATEGORIAS.find((c) => c.value === valor)?.label ?? valor;
}
