import type { Metadata } from "next";
import { ClientesDashboard } from "@/features/clientes/components/clientes-dashboard";

export const metadata: Metadata = {
  title: "Clientes | Hertz",
  description: "Consulta y gestiona fichas de clientes Hertz.",
};

export default function ClientesPage() {
  return <ClientesDashboard />;
}
