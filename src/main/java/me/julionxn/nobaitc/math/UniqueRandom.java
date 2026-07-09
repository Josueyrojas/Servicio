package me.julionxn.nobaitc.math;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generación de números aleatorios <b>sin repetición</b> dentro de un rango.
 *
 * <p>Una sola responsabilidad: muestreo aleatorio único. Se separó de las
 * operaciones matemáticas porque es la única pieza con estado (PRNG) y la
 * única con dos estrategias de rendimiento distintas.</p>
 */
public final class UniqueRandom {

    private static final ThreadLocal<Random> RANDOM = ThreadLocal.withInitial(Random::new);

    private UniqueRandom() { }

    /**
     * Devuelve {@code count} enteros distintos en [min, max] (ambos inclusive).
     *
     * <p>Elige automáticamente entre dos estrategias:</p>
     * <ul>
     *   <li><b>Fisher-Yates</b> cuando el rango cabe en {@code int} y se pide
     *       más del 50% de los valores posibles (evita colisiones lentas).</li>
     *   <li><b>HashSet de descarte</b> para rangos enormes ({@code long}) o
     *       selecciones pequeñas (evita materializar un pool gigante en RAM).</li>
     * </ul>
     *
     * @throws IllegalArgumentException si el rango es menor que {@code count}.
     */
    public static long[] sample(long min, long max, int count) {
        long range = max - min + 1;
        if (range < count) {
            throw new IllegalArgumentException(
                    "Rango insuficiente para generar " + count + " números únicos");
        }

        long[] result = new long[count];

        if (range <= Integer.MAX_VALUE && count > range * 0.5) {
            int intRange = (int) range;
            int[] pool = new int[intRange];
            for (int i = 0; i < intRange; i++) {
                pool[i] = (int) min + i;
            }
            Random random = RANDOM.get();
            for (int i = 0; i < count; i++) {
                int j = i + random.nextInt(intRange - i);
                result[i] = pool[j];
                pool[j] = pool[i];
            }
        } else {
            Set<Long> used = new HashSet<>(count);
            for (int i = 0; i < count; i++) {
                long num;
                do {
                    num = ThreadLocalRandom.current().nextLong(min, max + 1);
                } while (!used.add(num));
                result[i] = num;
            }
        }
        return result;
    }
}
