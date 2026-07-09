package me.julionxn.nobaitc.doe.alias;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Construye la matriz del modelo (efectos principales + interacciones dobles +
 * triples) a partir de la fracción, con normalización por columna.
 *
 * <p>Extraído de {@code AliasStructureGenerator.construirMatrizModelo} y
 * {@code multiplicarColumnas}. Responsabilidad única: ensamblar la matriz de
 * efectos sobre la que luego se calculan correlaciones.</p>
 *
 * <p>Normalización por columna: {@code 1 - (2*(Nj - x_ij) / (Nj - 1))}, donde
 * Nj es el nivel máximo de la columna j.</p>
 */
final class ModelMatrixBuilder {

    private final double[][] fraction;
    private final int rows;
    private final int factors;

    ModelMatrixBuilder(double[][] fraction) {
        this.fraction = fraction;
        this.rows = fraction.length;
        this.factors = fraction[0].length;
    }

    double[][] build() {
        double[][] normalizedCols = normalizeColumns();
        List<double[]> columns = new ArrayList<>();

        // Efectos principales
        for (int i = 0; i < factors; i++) {
            columns.add(normalizedCols[i]);
        }
        // Interacciones dobles
        for (int i = 0; i < factors; i++) {
            for (int j = i + 1; j < factors; j++) {
                columns.add(multiply(normalizedCols[i], normalizedCols[j]));
            }
        }
        // Interacciones triples
        if (factors > 2) {
            for (int i = 0; i < factors; i++) {
                for (int j = i + 1; j < factors; j++) {
                    for (int k = j + 1; k < factors; k++) {
                        double[] doubleIj = multiply(normalizedCols[i], normalizedCols[j]);
                        columns.add(multiply(doubleIj, normalizedCols[k]));
                    }
                }
            }
        }

        double[][] result = new double[rows][columns.size()];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns.size(); j++) {
                result[i][j] = columns.get(j)[i];
            }
        }
        return result;
    }

    private double[][] normalizeColumns() {
        int[] maxByColumn = new int[factors];
        for (int j = 0; j < factors; j++) {
            int max = (int) fraction[0][j];
            for (int i = 1; i < rows; i++) {
                if ((int) fraction[i][j] > max) {
                    max = (int) fraction[i][j];
                }
            }
            maxByColumn[j] = max;
        }

        double[][] cols = new double[factors][rows];
        for (int j = 0; j < factors; j++) {
            double nj = maxByColumn[j];
            if (nj == 1) {
                // Factor con un solo nivel: columna constante cero (evita div/0)
                Arrays.fill(cols[j], 0.0);
            } else {
                for (int i = 0; i < rows; i++) {
                    cols[j][i] = 1.0 - (2.0 * (nj - fraction[i][j]) / (nj - 1.0));
                }
            }
        }
        return cols;
    }

    private double[] multiply(double[] a, double[] b) {
        double[] res = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            res[i] = a[i] * b[i];
        }
        return res;
    }
}
