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

export function etiquetaSucursal(valor: string): string {
  return SUCURSALES.find((s) => s.value === valor)?.label ?? valor;
}

export function etiquetaCategoria(valor: string): string {
  return CATEGORIAS.find((c) => c.value === valor)?.label ?? valor;
}
