package me.julionxn.nobaitc.doe.metrics;

import me.julionxn.nobaitc.doe.design.Design;

/**
 * Métrica que evalúa la calidad de una fracción (patrón Strategy).
 *
 * <p>Cada métrica (GBM, J2, VIFS, …) es una implementación independiente.
 * Para añadir una métrica nueva basta con crear una clase que implemente esta
 * interfaz y registrarla; el orquestador {@code NonbpaGenerator} la recorrerá
 * sin necesidad de modificarse. Así se cumple el requisito de escalabilidad:
 * "añadir nuevos tipos de operaciones matriciales sin romper el flujo".</p>
 */
public interface FractionMetric {

    /** Nombre legible de la métrica (p. ej. "GBM", "J2", "VIFS"). */
    String name();

    /**
     * Evalúa la métrica sobre una fracción.
     *
     * @param fraction matriz de la fracción [renglones][factores]
     * @param design   diseño al que pertenece la fracción
     * @return vector de resultados. Las métricas escalares devuelven un arreglo
     *         de longitud 1; las vectoriales (p. ej. GBM por factor, VIFS por
     *         columna) devuelven un valor por componente.
     */
    double[] evaluate(double[][] fraction, Design design);

    /** Reducción escalar por defecto: suma de los componentes. */
    default double scalar(double[][] fraction, Design design) {
        double[] v = evaluate(fraction, design);
        double sum = 0;
        for (double x : v) {
            sum += x;
        }
        return sum;
    }
}
