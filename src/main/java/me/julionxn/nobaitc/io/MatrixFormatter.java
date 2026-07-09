package me.julionxn.nobaitc.io;

/**
 * Único punto de serialización de matrices y vectores a texto.
 *
 * <p>Antes esta lógica estaba duplicada en {@code FormatHelper}, {@code ExcelWriter}
 * y {@code PdfReportWriter}, cada uno con su propio recorrido. Ahora vive aquí.</p>
 *
 * <p>Una sola responsabilidad: formatear estructuras numéricas a {@code String}.</p>
 */
public final class MatrixFormatter {

    private static final String[] LETTERS = {
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O"
    };

    private MatrixFormatter() { }

    /** Etiqueta de columna por índice (A, B, C, …), cíclica. */
    public static String columnLabel(int index) {
        return LETTERS[index % LETTERS.length];
    }

    /** Formatea un valor: entero si no tiene parte decimal, si no 4 decimales. */
    public static String number(double value) {
        if (value == (long) value) {
            return String.format("%d", (long) value);
        }
        return String.format("%.4f", value);
    }

    /** Vector entre corchetes: {@code [1, 2, 3]}. */
    public static String formatVector(double[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(number(vector[i]));
        }
        return sb.append("]").toString();
    }

    /** Matriz, una fila por línea, indentada. */
    public static String formatMatrix(double[][] matrix) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < matrix.length; i++) {
            sb.append("  ").append(formatVector(matrix[i]));
            sb.append(i < matrix.length - 1 ? ",\n" : "\n");
        }
        return sb.toString();
    }

    /** Lista vertical sin corchetes, separada por coma + salto de línea. */
    public static String formatVerticalList(double[] vector) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vector.length; i++) {
            sb.append(number(vector[i]));
            if (i < vector.length - 1) {
                sb.append(",\n");
            }
        }
        return sb.toString();
    }

    /** Matriz como filas separadas por tabuladores (formato pegable en Excel). */
    public static String toTabSeparated(double[][] matrix) {
        StringBuilder sb = new StringBuilder();
        for (double[] row : matrix) {
            for (int i = 0; i < row.length; i++) {
                sb.append(number(row[i]));
                sb.append(i != row.length - 1 ? "\t" : "\n");
            }
        }
        return sb.toString();
    }
}
