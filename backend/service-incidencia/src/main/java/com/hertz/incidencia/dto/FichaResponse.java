package com.hertz.incidencia.dto;

import com.hertz.incidencia.entity.FichaDanoExterno;
import com.hertz.incidencia.entity.FichaDanoInterno;
import com.hertz.incidencia.entity.FichaSeguimiento;
import com.hertz.incidencia.entity.FichaTaller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Ficha del taller con todo lo registrado. Si todavia no se guardo nada, trae los campos vacios. */
public record FichaResponse(
        UUID idIncidencia,
        String estadoIncidencia,
        LocalDate ingresoATaller,
        String diagnostico,
        Integer diasEstimados,
        LocalDate fechaSalidaPrevista,
        BigDecimal costoRepuestos,
        BigDecimal costoManoObra,
        String trabajoRealizado,
        BigDecimal costoFinal,
        String actualizadoPor,
        Instant actualizadoEn,
        List<DanoInterno> internos,
        List<DanoExterno> externos,
        List<Seguimiento> seguimiento) {

    public record DanoInterno(String sistema, String descripcion, String gravedad, BigDecimal costoEstimado) {
        public static DanoInterno de(FichaDanoInterno d) {
            return new DanoInterno(d.getSistema().name(), d.getDescripcion(), d.getGravedad().name(), d.getCostoEstimado());
        }
    }

    public record DanoExterno(String vista, BigDecimal posX, BigDecimal posY, String tipo, String descripcion,
                              String gravedad, BigDecimal costoEstimado) {
        public static DanoExterno de(FichaDanoExterno d) {
            return new DanoExterno(d.getVista().name(), d.getPosX(), d.getPosY(), d.getTipo().name(),
                    d.getDescripcion(), d.getGravedad().name(), d.getCostoEstimado());
        }
    }

    public record Seguimiento(String nota, String registradoPor, Instant registradoEn) {
        public static Seguimiento de(FichaSeguimiento s) {
            return new Seguimiento(s.getNota(), s.getRegistradoPor(), s.getRegistradoEn());
        }
    }

    public static FichaResponse de(UUID idIncidencia, String estadoIncidencia, LocalDate ingreso, FichaTaller ficha,
                                   List<FichaDanoInterno> internos, List<FichaDanoExterno> externos,
                                   List<FichaSeguimiento> seguimiento) {
        return new FichaResponse(
                idIncidencia, estadoIncidencia, ingreso,
                ficha == null ? null : ficha.getDiagnostico(),
                ficha == null ? null : ficha.getDiasEstimados(),
                ficha == null ? null : ficha.getFechaSalidaPrevista(),
                ficha == null ? null : ficha.getCostoRepuestos(),
                ficha == null ? null : ficha.getCostoManoObra(),
                ficha == null ? null : ficha.getTrabajoRealizado(),
                ficha == null ? null : ficha.getCostoFinal(),
                ficha == null ? null : ficha.getActualizadoPor(),
                ficha == null ? null : ficha.getActualizadoEn(),
                internos.stream().map(DanoInterno::de).toList(),
                externos.stream().map(DanoExterno::de).toList(),
                seguimiento.stream().map(Seguimiento::de).toList());
    }
}
