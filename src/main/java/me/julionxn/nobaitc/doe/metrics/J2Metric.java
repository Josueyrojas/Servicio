package me.julionxn.nobaitc.doe.metrics;

import me.julionxn.nobaitc.doe.design.Design;

/**
 * Parámetro de ortogonalidad J2: penaliza renglones con niveles coincidentes.
 *
 * <p>Para cada par de renglones cuenta cuántos factores coinciden y acumula el
 * cuadrado de esas coincidencias. Cuanto menor es J2, más ortogonal es la
 * fracción.</p>
 *
 * <p>Equivale a la antigua clase {@code OrthogonalJ2Matrix}. Métrica escalar:
 * el vector resultante tiene longitud 1.</p>
 */
public final class J2Metric implements FractionMetric {

    @Override
    public String name() {
        return "J2";
    }

    @Override
    public double[] evaluate(double[][] fraction, Design design) {
        return new double[] { calculate(fraction) };
    }

    private double calculate(double[][] fraction) {
        int rows = fraction.length;
        int factors = fraction[0].length;
        double j2Total = 0;
        for (int i = 0; i < rows - 1; i++) {
            double[] row1 = fraction[i];
            for (int j = i + 1; j < rows; j++) {
                int matches = countMatches(row1, fraction[j], factors);
                j2Total += (double) matches * matches;
            }
        }
        return j2Total;
    }

    private int countMatches(double[] row1, double[] row2, int length) {
        int matches = 0;
        for (int i = 0; i < length; i++) {
            if (row1[i] == row2[i]) {
                matches++;
            }
        }
        return matches;
    }
}
