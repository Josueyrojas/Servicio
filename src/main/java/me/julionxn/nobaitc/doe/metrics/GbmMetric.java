package me.julionxn.nobaitc.doe.metrics;

import me.julionxn.nobaitc.doe.design.Design;
import me.julionxn.nobaitc.math.VectorOps;
import me.julionxn.nobaitc.math.LinearAlgebra;

/**
 * GBM (Generalized Balance Metric): mide el desbalance de niveles por factor.
 *
 * <p>Para cada factor compara el conteo real de cada nivel contra el conteo
 * esperado (renglones / niveles) y acumula las diferencias al cuadrado.
 * El GBM total (escalar) es la suma del vector por factor.</p>
 *
 * <p>Equivale a la antigua clase {@code BalancedGBMMatrix}.</p>
 */
public final class GbmMetric implements FractionMetric {

    @Override
    public String name() {
        return "GBM";
    }

    /** Devuelve el GBM de cada factor (un componente por columna). */
    @Override
    public double[] evaluate(double[][] fraction, Design design) {
        int rows = fraction.length;
        int factors = fraction[0].length;
        int[] levelsPerFactor = design.levels();
        double[] gbmPerFactor = new double[factors];

        for (int factor = 0; factor < factors; factor++) {
            int levels = levelsPerFactor[factor];
            double expectedCount = (double) rows / levels;
            double[] column = LinearAlgebra.extractColumn(fraction, factor);
            double factorGbm = 0;
            for (int level = 1; level <= levels; level++) {
                int actualCount = VectorOps.countOccurrences(column, level);
                factorGbm += VectorOps.squaredDifference(actualCount, expectedCount);
            }
            gbmPerFactor[factor] = factorGbm;
        }
        return gbmPerFactor;
    }
}
