import { Suspense } from "react";
import type { Metadata } from "next";
import { ReservasPage } from "@/features/reservas/components/reservas-page";

export const metadata: Metadata = {
  title: "Hertz | Reservas",
  description: "Crea, modifica y cancela reservas de vehículos Hertz.",
};

export default function Reservas() {
  // ReservasPage lee la dirección (?nueva=1&...), lo que exige un límite Suspense.
  return (
    <Suspense>
      <ReservasPage />
    </Suspense>
  );
}
