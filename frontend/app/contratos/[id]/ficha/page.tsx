import type { Metadata } from "next";
import { FichaContratoPage } from "@/features/contratos/components/ficha-contrato-page";

export const metadata: Metadata = {
  title: "Hertz | Ficha de entrega y recepción",
  description: "Ficha de entrega y recepción del vehículo de un contrato de alquiler.",
};

export default function FichaContrato() {
  return <FichaContratoPage />;
}
