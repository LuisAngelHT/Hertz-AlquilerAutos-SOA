"use client";

import type { MouseEvent, ReactElement } from "react";
import { VISTAS } from "@/features/contratos/lib/revision";
import type { VistaVehiculo } from "@/features/contratos/types/contrato";

export type TipoCarroceria = "SEDAN" | "SUV" | "PICKUP";

/** Qué silueta usar según la categoría del vehículo. */
export function tipoCarroceria(categoria: string | null | undefined): TipoCarroceria {
  switch ((categoria ?? "").toUpperCase()) {
    case "CAMIONETA":
      return "PICKUP";
    case "SUV":
    case "PREMIUM":
      return "SUV";
    default:
      return "SEDAN";
  }
}

/** Una marca de daño sobre una vista. x e y van de 0 a 100 (porcentaje del ancho y del alto del dibujo). */
export interface PinVista {
  id: string | number;
  vista: VistaVehiculo;
  x: number;
  y: number;
  /** Número (daños de esta revisión) o letra (daños que ya tenía al entregar). */
  numero: number | string;
  /** previo: ya estaba al entregar · nuevo: lo causó el cliente · taller: lo agregó el mecánico */
  tono: "previo" | "nuevo" | "taller";
  seleccionado?: boolean;
  titulo?: string;
}

const TRAZO = "#3f3f46";
const CARROCERIA = "#fafafa";
const VIDRIO = "#e0f2fe";
const LLANTA = "#27272a";
const RIN = "#d4d4d8";
const COLORES_PIN: Record<PinVista["tono"], string> = { previo: "#71717a", nuevo: "#dc2626", taller: "#ea580c" };

const ANCHO = 400;
const ALTO = 180;

// ---------------------------------------------------------------- dibujos (el vehículo mira hacia la izquierda)

interface GeometriaLateral {
  contorno: string;
  ventanas: string[];
  lineaPuerta: number;
  extras?: ReactElement;
}

function geometriaLateral(tipo: TipoCarroceria): GeometriaLateral {
  switch (tipo) {
    case "SUV":
      return {
        contorno:
          "M24 138 L24 110 Q24 100 38 98 L98 92 L146 86 L172 50 Q176 46 186 46 L318 46 Q330 46 336 54 L366 94 Q376 100 376 110 L376 138 Z",
        ventanas: [
          "M150 86 L174 54 L214 54 L214 86 Z",
          "M220 54 L262 54 L262 86 L220 86 Z",
          "M268 54 L320 54 Q326 54 330 60 L352 86 L268 86 Z",
        ],
        lineaPuerta: 217,
        extras: <path d="M182 46 L316 46" stroke={TRAZO} strokeWidth={4} strokeLinecap="round" />,
      };
    case "PICKUP":
      return {
        contorno:
          "M24 138 L24 110 Q24 100 38 98 L96 92 L130 86 L150 54 Q154 48 164 48 L232 48 Q242 48 246 56 L258 86 L376 86 L376 138 Z",
        ventanas: ["M134 86 L152 58 L192 58 L192 86 Z", "M198 58 L232 58 Q238 58 241 64 L252 86 L198 86 Z"],
        lineaPuerta: 195,
        extras: (
          <>
            <path d="M262 86 L262 104 M262 104 L376 104" stroke={TRAZO} strokeWidth={1.5} fill="none" />
            <path d="M370 86 L370 138" stroke={TRAZO} strokeWidth={1.5} />
            <rect x={332} y={112} width={26} height={5} rx={2} fill={RIN} stroke={TRAZO} strokeWidth={1} />
          </>
        ),
      };
    default:
      return {
        contorno:
          "M24 138 L24 112 Q24 102 38 100 L96 94 L150 88 L178 56 Q182 52 190 52 L236 52 Q246 52 252 58 L282 90 L358 96 Q376 98 376 110 L376 138 Z",
        ventanas: ["M156 88 L181 60 L214 60 L214 88 Z", "M222 60 L240 60 Q246 60 250 64 L274 88 L222 88 Z"],
        lineaPuerta: 218,
      };
  }
}

function Lateral({ tipo }: { tipo: TipoCarroceria }) {
  const g = geometriaLateral(tipo);
  return (
    <g>
      <line x1={10} y1={152} x2={390} y2={152} stroke="#d4d4d8" strokeWidth={1} />
      <path d={g.contorno} fill={CARROCERIA} stroke={TRAZO} strokeWidth={2} strokeLinejoin="round" />
      {g.ventanas.map((d) => (
        <path key={d} d={d} fill={VIDRIO} stroke={TRAZO} strokeWidth={1.5} strokeLinejoin="round" />
      ))}
      <line x1={g.lineaPuerta} y1={60} x2={g.lineaPuerta} y2={134} stroke={TRAZO} strokeWidth={1.2} />
      <rect x={g.lineaPuerta - 26} y={94} width={14} height={4} rx={2} fill={RIN} stroke={TRAZO} strokeWidth={0.8} />
      <rect x={g.lineaPuerta + 12} y={94} width={14} height={4} rx={2} fill={RIN} stroke={TRAZO} strokeWidth={0.8} />
      {g.extras}
      <ellipse cx={34} cy={108} rx={9} ry={5} fill="#fef3c7" stroke={TRAZO} strokeWidth={1.2} />
      <rect x={366} y={tipo === "PICKUP" ? 92 : 102} width={10} height={9} rx={2} fill="#fecaca" stroke={TRAZO} strokeWidth={1.2} />
      {[95, 305].map((cx) => (
        <g key={cx}>
          <path d={`M${cx - 33} 138 A33 33 0 0 1 ${cx + 33} 138`} fill="white" stroke={TRAZO} strokeWidth={2} />
          <circle cx={cx} cy={126} r={26} fill={LLANTA} />
          <circle cx={cx} cy={126} r={13} fill={RIN} stroke={TRAZO} strokeWidth={1.5} />
          <circle cx={cx} cy={126} r={4} fill={LLANTA} />
        </g>
      ))}
    </g>
  );
}

function Superior({ tipo }: { tipo: TipoCarroceria }) {
  const rueda = (x: number, y: number) => <rect key={`${x}-${y}`} x={x} y={y} width={44} height={13} rx={4} fill={LLANTA} />;
  const g =
    tipo === "SUV"
      ? { parabrisas: [118, 150], techo: [150, 302], luneta: [302, 330], espejo: 140 }
      : tipo === "PICKUP"
        ? { parabrisas: [116, 146], techo: [146, 216], luneta: [216, 230], espejo: 134 }
        : { parabrisas: [128, 160], techo: [160, 244], luneta: [244, 274], espejo: 150 };
  return (
    <g>
      {rueda(66, 26)}
      {rueda(66, 141)}
      {rueda(284, 26)}
      {rueda(284, 141)}
      <rect x={24} y={38} width={352} height={104} rx={28} fill={CARROCERIA} stroke={TRAZO} strokeWidth={2} />
      <path
        d={`M${g.parabrisas[0]} 52 L${g.parabrisas[1]} 58 L${g.parabrisas[1]} 122 L${g.parabrisas[0]} 128 Z`}
        fill={VIDRIO}
        stroke={TRAZO}
        strokeWidth={1.5}
        strokeLinejoin="round"
      />
      <rect
        x={g.techo[0]}
        y={58}
        width={g.techo[1] - g.techo[0]}
        height={64}
        rx={8}
        fill="#f4f4f5"
        stroke={TRAZO}
        strokeWidth={1.5}
      />
      <path
        d={`M${g.luneta[0]} 58 L${g.luneta[1]} 52 L${g.luneta[1]} 128 L${g.luneta[0]} 122 Z`}
        fill={VIDRIO}
        stroke={TRAZO}
        strokeWidth={1.5}
        strokeLinejoin="round"
      />
      {tipo === "PICKUP" ? (
        <>
          <rect x={238} y={48} width={130} height={84} rx={6} fill="#f4f4f5" stroke={TRAZO} strokeWidth={1.5} />
          <rect x={248} y={58} width={110} height={64} rx={4} fill="none" stroke={TRAZO} strokeWidth={1} />
        </>
      ) : (
        <line x1={tipo === "SUV" ? 346 : 294} y1={52} x2={tipo === "SUV" ? 346 : 294} y2={128} stroke={TRAZO} strokeWidth={1} />
      )}
      <ellipse cx={g.espejo} cy={34} rx={11} ry={5} fill={CARROCERIA} stroke={TRAZO} strokeWidth={1.5} />
      <ellipse cx={g.espejo} cy={146} rx={11} ry={5} fill={CARROCERIA} stroke={TRAZO} strokeWidth={1.5} />
      <rect x={24} y={62} width={12} height={10} rx={3} fill="#fef3c7" stroke={TRAZO} strokeWidth={1} />
      <rect x={24} y={108} width={12} height={10} rx={3} fill="#fef3c7" stroke={TRAZO} strokeWidth={1} />
    </g>
  );
}

/** Frente o parte trasera: la misma silueta con luces y rejilla (frente) o luneta y placa (atrás). */
function Frontal({ tipo, atras }: { tipo: TipoCarroceria; atras: boolean }) {
  const techo = tipo === "SEDAN" ? 54 : 42;
  const cuerpo =
    tipo === "SEDAN"
      ? `M110 142 L110 106 Q110 94 124 92 L148 88 Q160 ${techo} 176 ${techo} L224 ${techo} Q240 ${techo} 252 88 L276 92 Q290 94 290 106 L290 142 Z`
      : tipo === "SUV"
        ? `M104 142 L104 102 Q104 90 118 88 L142 84 Q150 ${techo} 170 ${techo} L230 ${techo} Q250 ${techo} 258 84 L282 88 Q296 90 296 102 L296 142 Z`
        : `M104 142 L104 102 Q104 92 116 90 L138 86 L150 ${techo + 4} Q154 ${techo} 164 ${techo} L236 ${techo} Q246 ${techo} 250 ${techo + 4} L262 86 L284 90 Q296 92 296 102 L296 142 Z`;
  const vidrio =
    tipo === "SEDAN"
      ? "M156 86 Q166 62 178 60 L222 60 Q234 62 244 86 Z"
      : "M152 82 Q160 52 174 50 L226 50 Q240 52 248 82 Z";
  return (
    <g>
      <line x1={60} y1={158} x2={340} y2={158} stroke="#d4d4d8" strokeWidth={1} />
      <rect x={108} y={138} width={18} height={20} rx={3} fill={LLANTA} />
      <rect x={274} y={138} width={18} height={20} rx={3} fill={LLANTA} />
      <path d={cuerpo} fill={CARROCERIA} stroke={TRAZO} strokeWidth={2} strokeLinejoin="round" />
      <path d={vidrio} fill={VIDRIO} stroke={TRAZO} strokeWidth={1.5} strokeLinejoin="round" />
      <ellipse cx={100} cy={92} rx={8} ry={5} fill={CARROCERIA} stroke={TRAZO} strokeWidth={1.5} />
      <ellipse cx={300} cy={92} rx={8} ry={5} fill={CARROCERIA} stroke={TRAZO} strokeWidth={1.5} />
      {atras ? (
        <>
          {tipo === "PICKUP" ? (
            <rect x={120} y={96} width={160} height={30} rx={4} fill="none" stroke={TRAZO} strokeWidth={1.5} />
          ) : null}
          <rect x={116} y={100} width={28} height={12} rx={3} fill="#fecaca" stroke={TRAZO} strokeWidth={1.2} />
          <rect x={256} y={100} width={28} height={12} rx={3} fill="#fecaca" stroke={TRAZO} strokeWidth={1.2} />
        </>
      ) : (
        <>
          <rect x={118} y={98} width={34} height={13} rx={4} fill="#fef3c7" stroke={TRAZO} strokeWidth={1.2} />
          <rect x={248} y={98} width={34} height={13} rx={4} fill="#fef3c7" stroke={TRAZO} strokeWidth={1.2} />
          <rect x={168} y={102} width={64} height={18} rx={3} fill="#e4e4e7" stroke={TRAZO} strokeWidth={1.2} />
          {[108, 114].map((y) => (
            <line key={y} x1={172} y1={y} x2={228} y2={y} stroke={TRAZO} strokeWidth={0.8} />
          ))}
        </>
      )}
      <rect x={112} y={124} width={176} height={14} rx={5} fill="#e4e4e7" stroke={TRAZO} strokeWidth={1.2} />
      <rect x={176} y={126} width={48} height={11} rx={2} fill="white" stroke={TRAZO} strokeWidth={1} />
      <text x={200} y={135} fontSize={8} textAnchor="middle" fill={TRAZO} fontFamily="monospace">
        {atras ? "ABC-123" : "ABC-123"}
      </text>
    </g>
  );
}

function Dibujo({ tipo, vista }: { tipo: TipoCarroceria; vista: VistaVehiculo }) {
  switch (vista) {
    case "IZQUIERDA":
      return <Lateral tipo={tipo} />;
    case "DERECHA":
      // Visto desde la derecha el vehículo mira hacia el otro lado: se refleja el mismo dibujo.
      return (
        <g transform={`translate(${ANCHO} 0) scale(-1 1)`}>
          <Lateral tipo={tipo} />
        </g>
      );
    case "FRENTE":
      return <Frontal tipo={tipo} atras={false} />;
    case "ATRAS":
      return <Frontal tipo={tipo} atras />;
    default:
      return <Superior tipo={tipo} />;
  }
}

// ---------------------------------------------------------------- una vista (con marcas)

interface VehiculoVistaProps {
  tipo: TipoCarroceria;
  vista: VistaVehiculo;
  pines: PinVista[];
  /** Si se pasa, se puede hacer clic en el dibujo para marcar un daño (x e y de 0 a 100). */
  onColocar?: (vista: VistaVehiculo, x: number, y: number) => void;
  onSeleccionar?: (id: string | number) => void;
  titulo?: string;
}

export function VehiculoVista({ tipo, vista, pines, onColocar, onSeleccionar, titulo }: VehiculoVistaProps) {
  const propios = pines.filter((p) => p.vista === vista);

  function alHacerClic(event: MouseEvent<SVGSVGElement>) {
    if (!onColocar) return;
    const caja = event.currentTarget.getBoundingClientRect();
    const x = ((event.clientX - caja.left) / caja.width) * 100;
    const y = ((event.clientY - caja.top) / caja.height) * 100;
    onColocar(vista, Math.min(100, Math.max(0, x)), Math.min(100, Math.max(0, y)));
  }

  return (
    <figure className="m-0">
      <figcaption className="mb-1 text-center text-[11px] font-semibold uppercase tracking-wide text-zinc-500">
        {titulo ?? VISTAS.find((v) => v.value === vista)?.label}
      </figcaption>
      <svg
        viewBox={`0 0 ${ANCHO} ${ALTO}`}
        role="img"
        aria-label={`Dibujo del vehículo: ${VISTAS.find((v) => v.value === vista)?.label}${onColocar ? ". Haz clic para marcar un daño" : ""}`}
        onClick={alHacerClic}
        className={`block w-full rounded-xl bg-white ring-1 ring-zinc-200 ${onColocar ? "cursor-crosshair" : ""}`}
      >
        <Dibujo tipo={tipo} vista={vista} />
        {propios.map((pin) => {
          const cx = (pin.x / 100) * ANCHO;
          const cy = (pin.y / 100) * ALTO;
          return (
            <g
              key={pin.id}
              onClick={(event) => {
                if (!onSeleccionar) return;
                event.stopPropagation();
                onSeleccionar(pin.id);
              }}
              className={onSeleccionar ? "cursor-pointer" : undefined}
            >
              <title>{pin.titulo ?? `Daño ${pin.numero}`}</title>
              {pin.seleccionado && <circle cx={cx} cy={cy} r={15} fill="none" stroke="#f59e0b" strokeWidth={3} />}
              <circle cx={cx} cy={cy} r={10} fill={COLORES_PIN[pin.tono]} stroke="white" strokeWidth={2} />
              <text x={cx} y={cy + 3.6} fontSize={11} fontWeight={700} textAnchor="middle" fill="white" fontFamily="sans-serif">
                {pin.numero}
              </text>
            </g>
          );
        })}
      </svg>
    </figure>
  );
}

// ---------------------------------------------------------------- las cinco vistas juntas

interface VehiculoVistasProps {
  tipo: TipoCarroceria;
  pines: PinVista[];
  onColocar?: (vista: VistaVehiculo, x: number, y: number) => void;
  onSeleccionar?: (id: string | number) => void;
  /** Cuántas vistas por fila en pantallas anchas (2 por defecto; 3 para fichas amplias). */
  columnas?: 2 | 3;
}

export function VehiculoVistas({ tipo, pines, onColocar, onSeleccionar, columnas = 2 }: VehiculoVistasProps) {
  return (
    <div className={`grid gap-3 ${columnas === 3 ? "sm:grid-cols-3" : "sm:grid-cols-2"}`}>
      {VISTAS.map((v) => (
        <VehiculoVista
          key={v.value}
          tipo={tipo}
          vista={v.value}
          pines={pines}
          onColocar={onColocar}
          onSeleccionar={onSeleccionar}
        />
      ))}
    </div>
  );
}
