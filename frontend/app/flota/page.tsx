import type { Metadata } from "next";
import { FlotaPage } from "@/features/flota/components/flota-page";

export const metadata: Metadata = {
  title: "Hertz | Flota",
  description: "Disponibilidad y estado de los vehículos de la flota Hertz.",
};

export default function Flota() {
  return <FlotaPage />;
}
