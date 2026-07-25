package me.julionxn.nobaitc.doe.alias;

import me.julionxn.nobaitc.stats.Correlation;

import java.util.ArrayList;
import java.util.List;

/**
 * Calcula la estructura de alias de una fracción factorial.
 *
 * <p>Implementa el algoritmo de <i>Ríos-Lira et al., "Alias Structures and
 * Sequential Experimentation for Mixed-Level Designs"</i>, Mathematics 2021,
 * 9(23), 3053 (<a href="https://doi.org/10.3390/math9233053">DOI</a>):
 * emparejamiento voraz global sobre la matriz de correlaciones.</p>
 *
 * <p>El método NO evalúa cada efecto de forma independiente. En su lugar:</p>
 * <ol>
 *   <li>Se listan TODAS las correlaciones por pares entre efectos (principales,
 *       dobles y triples), sin importar entre qué tipos de efecto sean.</li>
 *   <li>Se ordenan de mayor a menor valor absoluto.</li>
 *   <li>Se recorren en ese orden; el primer par cuyos DOS efectos siguen libres
 *       forma una cadena de alias y ambos quedan marcados como usados
 *       ("the same term cannot be included in multiple alias chains").
 *       El efecto de menor orden (principal &lt; doble &lt; triple; y dentro del
 *       mismo orden, el que aparece primero en la enumeración A, B, …, AB, AC, …)
 *       queda como "cabeza" de la cadena; el otro es su alias.</li>
 *   <li>Se continúa hasta agotar la lista. Cualquier efecto que nunca quede
 *       libre-con-otro-libre-y-correlacionado termina como cadena de un solo
 *       elemento (sin alias).</li>
 * </ol>
 *
 * <p><b>Por qué se reescribió:</b> la versión anterior (portada de {@code PASO4.m}
 * / {@code PASO5.m}) procesaba cada fila/columna de forma independiente contra
 * un umbral VL, lo que permitía que un mismo efecto principal recibiera alias
 * de MÁS DE UNA interacción a la vez (p. ej. A aliado simultáneamente con ABC
 * Y con BCD) mientras otro efecto con una correlación aún más fuerte (p. ej.
 * B–BDE) se quedaba sin ningún alias — contradice justo la regla central del
 * paper ("each term should appear in only one chain so that only the highest
 * and most important correlations were included"). Verificado reproduciendo a
 * mano el Ejemplo de la Tabla 2 del paper (EA(15, 3¹5¹7¹)): con este algoritmo
 * se obtienen exactamente las 3 cadenas reportadas — [AB]=AB+0.998·ABC,
 * [B]=B+0.980·BC, [A]=A+0.938·AC — dejando C sin alias, igual que el paper.</p>
 */
public class AliasAnalyzer {

    /**
     * Umbral heredado del guard de Matlab ("efectos principales fuertemente
     * correlacionados"). Es inalcanzable para una correlación de Pearson
     * (rango [-1, 1]), por lo que esta validación nunca se dispara con datos
     * reales; se conserva tal cual para no alterar el comportamiento ya
     * validado del mensaje de error.
     */
    private static final double STRONG_CORRELATION_THRESHOLD = 1.5;

    /** Correlaciones con |r| por debajo de esto se tratan como "sin relación". */
    private static final double ZERO_TOL = 0.0001;

    private final double[][] fraction;
    private final int n; // factores

    private String[] effectNames;
    private String[][] letterMatrix;
    private int mainEffects;

    public AliasAnalyzer(double[][] fraction) {
        if (fraction == null || fraction.length == 0) {
            throw new IllegalArgumentException("La fracción no puede estar vacía");
        }
        this.fraction = fraction;
        this.n = fraction[0].length;
    }

    /**
     * Genera la estructura de alias.
     *
     * @throws IllegalStateException si los efectos principales están fuertemente
     *         correlacionados (ver nota de revisión sobre el guard).
     */
    public AliasStructure generate() {
        double[][] correlations = computeCorrelations();
        sanitizeNaN(correlations);

        if (hasStrongMainEffectCorrelations(correlations)) {
            throw new IllegalStateException(
                    "La fracción contiene efectos principales que están fuertemente "
                    + "correlacionados. No se puede calcular la estructura de alias.");
        }

        double[][] msz = buildAliasMatrix(correlations);
        return new AliasStructure(msz, effectNames, letterMatrix, mainEffects);
    }

    // ── Correlaciones ────────────────────────────────────────────────────

    private double[][] computeCorrelations() {
        EffectLabeler labeler = new EffectLabeler(n);
        this.effectNames = labeler.effectNames();
        this.letterMatrix = labeler.letterMatrix();
        this.mainEffects = n;

        double[][] modelMatrix = new ModelMatrixBuilder(fraction).build();
        return Correlation.corrcoef(modelMatrix);
    }

    /**
     * Una columna del modelo perfectamente confundida (varianza 0 — p. ej. una
     * triple totalmente aliada) hace que {@code corrcoef} devuelva NaN. Una
     * correlación indefinida NO es un alias: se sanea a 0 para que no se cuele
     * en el ordenamiento ni en el emparejamiento.
     */
    private void sanitizeNaN(double[][] correlations) {
        for (double[] row : correlations) {
            for (int j = 0; j < row.length; j++) {
                if (Double.isNaN(row[j])) {
                    row[j] = 0.0;
                }
            }
        }
    }

    private boolean hasStrongMainEffectCorrelations(double[][] correlations) {
        for (int i = 0; i < mainEffects; i++) {
            for (int j = i + 1; j < mainEffects; j++) {
                if (Math.abs(correlations[i][j]) >= STRONG_CORRELATION_THRESHOLD) {
                    return true;
                }
            }
        }
        return false;
    }

    // ── Emparejamiento voraz global ─────────────────────────────────────────

    /**
     * Construye la matriz de alias: diagonal 1 para todo efecto, y a lo sumo un
     * valor fuera de la diagonal por cada efecto (su único alias, si lo tiene).
     * {@code msz[miembro][cabeza] = r} — misma convención que usaba la versión
     * anterior, así que {@link AliasStructure} no necesita cambios.
     */
    private double[][] buildAliasMatrix(double[][] correlations) {
        int l = effectNames.length;
        double[][] msz = new double[l][l];

        // Todos los pares (i, j) con i < j. Como effectNames enumera primero los
        // principales, luego los dobles y luego los triples (mismo orden que
        // ModelMatrixBuilder), i < j implica que el efecto i tiene orden menor o
        // igual que j — exactamente la "cabeza" que pide el paper.
        List<int[]> pairs = new ArrayList<>();
        for (int i = 0; i < l; i++) {
            for (int j = i + 1; j < l; j++) {
                pairs.add(new int[] {i, j});
            }
        }
        pairs.sort((p1, p2) -> Double.compare(
                Math.abs(correlations[p2[0]][p2[1]]),
                Math.abs(correlations[p1[0]][p1[1]])));

        boolean[] used = new boolean[l];
        // Un efecto que ya quedó como MIEMBRO de una cadena ajena no debe además
        // mostrarse como su propia cadena trivial "X = X" — ya aparece, con su
        // coeficiente, en la línea de su cabeza.
        boolean[] isMember = new boolean[l];
        for (int[] pair : pairs) {
            int head = pair[0];
            int member = pair[1];
            if (used[head] || used[member]) {
                continue;
            }
            double r = correlations[head][member];
            if (Math.abs(r) < ZERO_TOL) {
                continue;
            }
            used[head] = true;
            used[member] = true;
            isMember[member] = true;
            msz[member][head] = r;
        }

        for (int i = 0; i < l; i++) {
            if (!isMember[i]) {
                msz[i][i] = 1.0;
            }
        }
        return msz;
    }
}
