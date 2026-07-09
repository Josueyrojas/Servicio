package me.julionxn.nobaitc.math;

/**
 * Combinatoria y aritmética de enteros: GCD, LCM y coeficiente binomial.
 *
 * <p>Una sola responsabilidad: operaciones combinatorias/numéricas sobre enteros.
 * Antes vivían mezcladas dentro de {@code MatlabFunctions}.</p>
 *
 * <p>Clase de utilidad pura (sin estado, sin dependencias externas).</p>
 */
public final class Combinatorics {

    private Combinatorics() { }

    /**
     * Mínimo común múltiplo de un arreglo de enteros (usando long para evitar overflow).
     * @return 0 si el arreglo es nulo/vacío o contiene un 0.
     */
    public static long lcm(int[] numbers) {
        if (numbers == null || numbers.length == 0) {
            return 0L;
        }
        long result = numbers[0];
        for (int i = 1; i < numbers.length; i++) {
            result = lcm(result, numbers[i]);
        }
        return result;
    }

    /** LCM de dos longs. Divide primero por el GCD para minimizar overflow. */
    public static long lcm(long a, long b) {
        if (a == 0 || b == 0) {
            return 0L;
        }
        return (a / gcd(a, b)) * b;
    }

    /** Máximo común divisor (algoritmo de Euclides). */
    public static long gcd(long a, long b) {
        while (b != 0) {
            long temp = b;
            b = a % b;
            a = temp;
        }
        return Math.abs(a);
    }

    /** Coeficiente binomial C(n, k). Devuelve 0 si k está fuera de [0, n]. */
    public static int nchoosek(int n, int k) {
        if (k > n || k < 0) {
            return 0;
        }
        if (k == 0 || k == n) {
            return 1;
        }
        if (k > n - k) {
            k = n - k;
        }
        long result = 1;
        for (int i = 0; i < k; i++) {
            result = result * (n - i) / (i + 1);
        }
        return (int) result;
    }
}
