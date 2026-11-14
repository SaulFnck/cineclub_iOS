package com.sauldiaz.cineclub_api.util;

import com.sauldiaz.cineclub_api.modelo.Renta;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Reglas de negocio de las rentas, en un solo lugar.
 *  - Precio base: costo_renta de cada película (se guarda en la renta al crearla).
 *  - Plazo: 7 días.
 *  - Recargo: $25 por cada día calendario de retraso.
 */
public final class ReglasRenta {

    public static final int DIAS_PLAZO = 7;
    public static final BigDecimal RECARGO_POR_DIA = new BigDecimal("25.00");

    public static final String ACTIVA = "ACTIVA";
    public static final String VENCIDA = "VENCIDA";
    public static final String DEVUELTA = "DEVUELTA";

    private ReglasRenta() {
    }

    /** Días calendario de retraso (límite 15 nov, entrega 16 nov a cualquier hora = 1). */
    public static int diasRetraso(LocalDateTime limite, LocalDateTime referencia) {
        long dias = ChronoUnit.DAYS.between(limite.toLocalDate(), referencia.toLocalDate());
        return (int) Math.max(0, dias);
    }

    public static BigDecimal recargo(int diasRetraso) {
        return RECARGO_POR_DIA.multiply(BigDecimal.valueOf(diasRetraso));
    }

    /**
     * Llena estado, diasRetraso, recargo y montoActual.
     * En rentas activas, montoCobrado es el precio base; en devueltas ya incluye el recargo.
     */
    public static void calcularCampos(Renta r) {
        if (r.getFechaDevolucionReal() != null) {
            int dias = diasRetraso(r.getFechaLimiteDevolucion(), r.getFechaDevolucionReal());
            r.setEstado(DEVUELTA);
            r.setDiasRetraso(dias);
            r.setRecargo(recargo(dias));
            r.setMontoActual(r.getMontoCobrado());
        } else {
            int dias = diasRetraso(r.getFechaLimiteDevolucion(), LocalDateTime.now());
            r.setEstado(dias > 0 ? VENCIDA : ACTIVA);
            r.setDiasRetraso(dias);
            r.setRecargo(recargo(dias));
            r.setMontoActual(r.getMontoCobrado().add(recargo(dias)));
        }
    }
}
