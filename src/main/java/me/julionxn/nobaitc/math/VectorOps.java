package me.julionxn.nobaitc.math;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoublePredicate;

/**
 * Operaciones sobre vectores ({@code double[]} / {@code int[]}):
 * suma, media, máximo, argmax, desviación estándar, búsqueda y conteo.
 *
 * <p>Una sola responsabilidad: estadística descriptiva y reducciones sobre arreglos 1D.
 * Clase de utilidad pura.</p>
 */
public final class VectorOps {

    private VectorOps() { }

    public static double mean(double[] array) {
        if (array == null || array.length == 0) {
            return 0;
        }
        return sum(array) / array.length;
    }

    public static double sum(double[] array) {
        if (array == null || array.length == 0) {
            return 0;
        }
        double total = 0;
        for (double value : array) {
            total += value;
        }
        return total;
    }

    public static int sum(int[] array) {
        if (array == null || array.length == 0) {
            return 0;
        }
        int total = 0;
        for (int value : array) {
            total += value;
        }
        return total;
    }

    public static double max(double[] array) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException("Array vacío");
        }
        double max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > max) {
                max = array[i];
            }
        }
        return max;
    }

    /** Índice del valor máximo. Devuelve -1 para arreglo nulo/vacío. */
    public static int argmax(double[] array) {
        if (array == null || array.length == 0) {
            return -1;
        }
        int maxIdx = 0;
        double maxVal = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxVal) {
                maxVal = array[i];
                maxIdx = i;
            }
        }
        return maxIdx;
    }

    /** Índices que satisfacen la condición (equivalente a {@code find} de MATLAB). */
    public static int[] find(double[] array, DoublePredicate condition) {
        if (array == null) {
            return new int[0];
        }
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < array.length; i++) {
            if (condition.test(array[i])) {
                indices.add(i);
            }
        }
        return indices.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Valor máximo en el rango [start, end) del arreglo.
     * @return 0 si el rango es inválido.
     */
    public static double maxInRange(double[] array, int start, int end) {
        if (array == null || start >= end || start < 0) {
            return 0;
        }
        end = Math.min(end, array.length);
        double max = array[start];
        for (int i = start + 1; i < end; i++) {
            if (array[i] > max) {
                max = array[i];
            }
        }
        return max;
    }

    /** Cuenta ocurrencias exactas de {@code value}. */
    public static int countOccurrences(double[] array, double value) {
        int count = 0;
        for (double v : array) {
            if (v == value) {
                count++;
            }
        }
        return count;
    }

    /** Diferencia al cuadrado (helper para métricas de balance). */
    public static double squaredDifference(double actual, double expected) {
        double diff = actual - expected;
        return diff * diff;
    }

    /** Desviación estándar poblacional respecto de una media ya calculada. */
    public static double standardDeviation(double[] array, double mean) {
        double variance = 0;
        for (double value : array) {
            double diff = value - mean;
            variance += diff * diff;
        }
        return Math.sqrt(variance / array.length);
    }

    public static boolean isEmpty(int[] array) {
        return array == null || array.length == 0;
    }
}
