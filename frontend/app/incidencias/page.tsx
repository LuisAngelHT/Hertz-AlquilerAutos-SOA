import type { Metadata } from "next";
import { IncidenciasPage } from "@/features/incidencias/components/incidencias-page";

export const metadata: Metadata = {
  title: "Hertz | Incidencias",
  description: "Registro y consulta de incidencias de vehículos Hertz.",
};

export default function Incidencias() {
  return <IncidenciasPage />;
}
