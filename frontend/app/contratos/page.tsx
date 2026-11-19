import type { Metadata } from "next";
import { ContratosPage } from "@/features/contratos/components/contratos-page";

export const metadata: Metadata = {
  title: "Hertz | Contratos",
  description: "Consulta los contratos de alquiler de vehículos Hertz.",
};

export default function Contratos() {
  return <ContratosPage />;
}
