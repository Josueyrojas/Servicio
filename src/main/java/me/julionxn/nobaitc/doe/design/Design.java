package me.julionxn.nobaitc.doe.design;

import me.julionxn.nobaitc.math.Combinatorics;

import java.util.Arrays;

/**
 * Diseño factorial: niveles por factor, más su validación y parámetros derivados.
 *
 * <p>Value Object inmutable. Encapsula las reglas de validez NONBPA que antes
 * estaban repartidas dentro de {@code NONBPAGeneratorService}.</p>
 *
 * @param levels número de niveles de cada factor (longitud = número de factores)
 */
public record Design(int[] levels) {

    private static final int MAX_FACTORS = 15;

    public Design {
        if (levels == null) {
            throw new IllegalArgumentException("El diseño no puede ser nulo");
        }
        levels = levels.clone(); // inmutabilidad defensiva
    }

    public int factors() {
        return levels.length;
    }

    /** Número total de corridas (TR) = producto de los niveles. */
    public long totalRuns() {
        long product = 1L;
        for (int value : levels) {
            product *= value;
        }
        return product;
    }

    public long lcm() {
        return Combinatorics.lcm(levels);
    }

    /**
     * Indica si el diseño es válido para NONBPA: entre 1 y 15 factores, cada
     * factor con al menos 2 niveles, y TR == LCM.
     */
    public boolean isValid() {
        if (levels.length == 0 || levels.length > MAX_FACTORS) {
            return false;
        }
        for (int l : levels) {
            if (l < 2) {
                return false;
            }
        }
        return totalRuns() == lcm();
    }

    /** Parámetros derivados del diseño (TR, factores, LCM, GL, SF mínimo). */
    public DesignParameters parameters() {
        long tr = totalRuns();
        int factors = levels.length;
        long lcm = lcm();
        int gl = factors + 2;
        int maxLevel = Arrays.stream(levels).max().orElse(0);
        int sfMin = Math.max(gl, maxLevel);
        return new DesignParameters(tr, factors, lcm, gl, sfMin);
    }

    /** Valida que el tamaño de fracción esté en [SF min, TR). */
    public boolean isFractionSizeValid(int fractionSize) {
        DesignParameters p = parameters();
        return fractionSize >= p.sfMin() && fractionSize < p.tr();
    }

    @Override
    public String toString() {
        return "Design" + Arrays.toString(levels);
    }

    /** Parámetros derivados de un diseño. */
    public record DesignParameters(long tr, int factors, long lcm, int gl, int sfMin) { }
}
