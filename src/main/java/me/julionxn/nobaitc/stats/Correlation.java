package me.julionxn.nobaitc.stats;

import me.julionxn.nobaitc.math.LinearAlgebra;
import me.julionxn.nobaitc.math.VectorOps;

/**
 * Estadística de correlación: coeficiente de Pearson y matriz de correlaciones
 * ({@code corrcoef}).
 *
 * <p>Una sola responsabilidad: correlación. Se apoya en {@link VectorOps} y
 * {@link LinearAlgebra} de la capa {@code math}. Antes esta lógica estaba
 * mezclada dentro de {@code MatlabFunctions} junto con álgebra lineal.</p>
 */
public final class Correlation {

    private Correlation() { }

    /**
     * Matriz de correlación de Pearson entre columnas (equivale a {@code corrcoef}
     * de MATLAB sobre las columnas de {@code matrix}).
     *
     * @return matriz simétrica [cols][cols] con 1 en la diagonal.
     */
    public static double[][] corrcoef(double[][] matrix) {
        if (matrix == null || matrix.length == 0) {
            throw new IllegalArgumentException("Matriz vacía");
        }

        int cols = matrix[0].length;
        double[][] corrMatrix = new double[cols][cols];

        double[] means = new double[cols];
        double[] stdDevs = new double[cols];
        for (int j = 0; j < cols; j++) {
            double[] column = LinearAlgebra.extractColumn(matrix, j);
            means[j] = VectorOps.mean(column);
            stdDevs[j] = VectorOps.standardDeviation(column, means[j]);
        }

        for (int i = 0; i < cols; i++) {
            corrMatrix[i][i] = 1.0;
            for (int j = i + 1; j < cols; j++) {
                double corr = pearsonByColumn(matrix, i, j,
                        means[i], means[j], stdDevs[i], stdDevs[j]);
                corrMatrix[i][j] = corr;
                corrMatrix[j][i] = corr;
            }
        }
        return corrMatrix;
    }

    /** Correlación de Pearson entre dos vectores del mismo tamaño. */
    public static double pearson(double[] x, double[] y) {
        if (x.length != y.length || x.length == 0) {
            throw new IllegalArgumentException("Arrays inválidos");
        }
        int n = x.length;
        double meanX = VectorOps.mean(x);
        double meanY = VectorOps.mean(y);

        double covariance = 0;
        double varX = 0;
        double varY = 0;
        for (int i = 0; i < n; i++) {
            double diffX = x[i] - meanX;
            double diffY = y[i] - meanY;
            covariance += diffX * diffY;
            varX += diffX * diffX;
            varY += diffY * diffY;
        }
        double denominator = Math.sqrt(varX * varY);
        return denominator == 0 ? Double.NaN : covariance / denominator;
    }

    /** Versión optimizada que reutiliza medias y desviaciones ya calculadas. */
    private static double pearsonByColumn(double[][] matrix, int col1, int col2,
                                          double mean1, double mean2,
                                          double std1, double std2) {
        if (std1 == 0 || std2 == 0) {
            return Double.NaN;
        }
        int rows = matrix.length;
        double covariance = 0;
        for (int i = 0; i < rows; i++) {
            covariance += (matrix[i][col1] - mean1) * (matrix[i][col2] - mean2);
        }
        return covariance / (rows * std1 * std2);
    }
}
