package me.julionxn.nobaitc.math;

/**
 * Álgebra lineal pura sobre {@code double[][]}: transposición, inversa
 * (Gauss-Jordan con pivoteo parcial), diagonales, triangular inferior y
 * extracción de columnas.
 *
 * <p>Una sola responsabilidad: operaciones matriciales genéricas, sin
 * conocimiento de estadística ni del dominio NONBPA/alias.</p>
 *
 * <p>Todas las operaciones son <b>no destructivas</b>: nunca mutan la matriz
 * de entrada (la inversa trabaja sobre una copia aumentada).</p>
 */
public final class LinearAlgebra {

    private static final double EPSILON = 1e-10;

    private LinearAlgebra() { }

    public static double[][] transpose(double[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        double[][] result = new double[cols][rows];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result[j][i] = matrix[i][j];
            }
        }
        return result;
    }

    /** Extrae la columna {@code col} como vector. */
    public static double[] extractColumn(double[][] matrix, int col) {
        int rows = matrix.length;
        double[] column = new double[rows];
        for (int i = 0; i < rows; i++) {
            column[i] = matrix[i][col];
        }
        return column;
    }

    /** Máximo de la columna {@code col}. */
    public static double maxCol(double[][] matrix, int col) {
        if (matrix == null || matrix.length == 0) {
            throw new IllegalArgumentException("Matriz vacía");
        }
        double max = matrix[0][col];
        for (int i = 1; i < matrix.length; i++) {
            if (matrix[i][col] > max) {
                max = matrix[i][col];
            }
        }
        return max;
    }

    /**
     * Inversa por Gauss-Jordan con pivoteo parcial.
     *
     * @throws IllegalArgumentException si la matriz no es cuadrada.
     * @throws SingularMatrixException  si la matriz es singular (sin inversa).
     *         Antes este caso devolvía {@code null}; ahora se señaliza con excepción
     *         para unificar el manejo de errores con el resto del sistema.
     */
    public static double[][] inv(double[][] matrix) {
        int n = matrix.length;
        for (double[] row : matrix) {
            if (row.length != n) {
                throw new IllegalArgumentException("Matriz debe ser cuadrada");
            }
        }

        double[][] augmented = new double[n][2 * n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(matrix[i], 0, augmented[i], 0, n);
            augmented[i][n + i] = 1.0;
        }

        for (int i = 0; i < n; i++) {
            int maxRow = findPivotRow(augmented, i, n);
            if (maxRow != i) {
                swapRows(augmented, i, maxRow);
            }
            if (Math.abs(augmented[i][i]) < EPSILON) {
                throw new SingularMatrixException(
                        "La matriz es singular: no se pudo calcular la inversa (pivote ≈ 0 en la fila " + i + ").");
            }
            double pivot = augmented[i][i];
            multiplyRow(augmented[i], 1.0 / pivot);
            eliminateColumn(augmented, i, n);
        }

        double[][] inverse = new double[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(augmented[i], n, inverse[i], 0, n);
        }
        return inverse;
    }

    /** Matriz diagonal a partir de un vector. */
    public static double[][] diag(double[] vector) {
        int n = vector.length;
        double[][] result = new double[n][n];
        for (int i = 0; i < n; i++) {
            result[i][i] = vector[i];
        }
        return result;
    }

    /** Diagonal principal de una matriz como vector. */
    public static double[] diag(double[][] matrix) {
        int n = Math.min(matrix.length, matrix[0].length);
        double[] result = new double[n];
        for (int i = 0; i < n; i++) {
            result[i] = matrix[i][i];
        }
        return result;
    }

    /** Parte triangular inferior (incluida la diagonal); el resto se pone a 0. */
    public static double[][] tril(double[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        double[][] result = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j <= i && j < cols; j++) {
                result[i][j] = matrix[i][j];
            }
        }
        return result;
    }

    // ── Helpers de Gauss-Jordan ───────────────────────────────────────────

    private static int findPivotRow(double[][] matrix, int col, int n) {
        int maxRow = col;
        double maxVal = Math.abs(matrix[col][col]);
        for (int k = col + 1; k < n; k++) {
            double val = Math.abs(matrix[k][col]);
            if (val > maxVal) {
                maxVal = val;
                maxRow = k;
            }
        }
        return maxRow;
    }

    private static void swapRows(double[][] matrix, int row1, int row2) {
        double[] temp = matrix[row1];
        matrix[row1] = matrix[row2];
        matrix[row2] = temp;
    }

    private static void multiplyRow(double[] row, double scalar) {
        for (int j = 0; j < row.length; j++) {
            row[j] *= scalar;
        }
    }

    private static void eliminateColumn(double[][] matrix, int pivotRow, int n) {
        for (int k = 0; k < n; k++) {
            if (k != pivotRow) {
                double factor = matrix[k][pivotRow];
                for (int j = 0; j < matrix[k].length; j++) {
                    matrix[k][j] -= factor * matrix[pivotRow][j];
                }
            }
        }
    }
}
