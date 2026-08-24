package me.julionxn.nobaitc.doe.alias;

import me.julionxn.nobaitc.stats.Correlation;

/**
 * Calcula la estructura de alias de una fracción factorial.
 *
 * <p>Port fiel del código Matlab que acompaña a <i>Ríos-Lira et al., "Alias
 * Structures and Sequential Experimentation for Mixed-Level Designs"</i>,
 * Mathematics 2021, 9(23), 3053 (<a href="https://doi.org/10.3390/math9233053">DOI</a>):
 * {@code GENERADORESTRUCTURAALIASITC.m} → {@code PASO4.m} → {@code PASO5.m} →
 * {@code BUSCACORRELACIONESSUPERIORESALVL.m} →
 * {@code LOCALIZACORRELACIONESSUPERIORESALVL.m} →
 * {@code ASIGNACORRELACIONESINFERIORESALVL.m} → {@code CAMBIODESIGNOS.m}.</p>
 *
 * <p>El algoritmo trabaja sobre el TRIÁNGULO INFERIOR {@code T} de la matriz de
 * correlaciones (fila = efecto de índice mayor, columna = efecto de índice
 * menor, es decir la "cabeza" de la cadena) y procede así:</p>
 * <ol>
 *   <li><b>Umbral</b> (PASO4): {@code VL = 0.5 · max|T_ij|} sobre las entradas
 *       con {@code T_ij < 1} (excluye la diagonal). Es un umbral RELATIVO al
 *       diseño, no una constante.</li>
 *   <li><b>Filtro</b> (BUSCA…): {@code revW} se queda sólo con |correlaciones|
 *       ≥ VL.</li>
 *   <li><b>Asignación por renglón</b> (LOCALIZA…): cada efecto (renglón) se
 *       queda con UNA sola correlación — la mayor — buscándola por bloques y en
 *       orden de prioridad: primero entre los efectos principales, si no hay
 *       entre las interacciones dobles, y si no entre las triples. Al asignarse,
 *       la columna de ese efecto se anula: un efecto que ya es miembro de una
 *       cadena no puede además encabezar la suya.</li>
 *   <li><b>Rescate</b> (ASIGNA…): los efectos que quedaron sin nada por encima
 *       de VL recuperan su correlación más grande aunque esté POR DEBAJO del
 *       umbral, siempre que la cabeza candidata no sea a su vez miembro de otra
 *       cadena.</li>
 *   <li><b>Signos</b> (CAMBIODESIGNOS): las magnitudes seleccionadas se
 *       reemplazan por el valor con signo de {@code T}.</li>
 * </ol>
 *
 * <p><b>Consecuencia importante:</b> una cadena puede tener VARIOS alias
 * ({@code D = D  -0.245728·AE  -0.491510·BC  +0.266003·BCD}). Lo que el
 * método garantiza es que cada efecto aparece en UNA sola cadena — como miembro
 * o como cabeza, nunca en ambas —, no que cada cadena tenga un solo término.</p>
 *
 * <p><b>Validación:</b> reproduce exactamente, término a término y con seis
 * decimales, las corridas de Matlab del archivo
 * {@code EJERCICIOS_JOSUE 04.08.26.xlsx} (4, 5, 6 y 7 factores; 30, 35, 35 y 40
 * corridas; 52 cadenas y 143 términos en total), incluidas las cadenas de hasta
 * siete alias del ejercicio 4.</p>
 *
 * <p><b>Única desviación deliberada respecto a Matlab:</b> para descartar la
 * diagonal, {@code BUSCACORRELACIONESSUPERIORESALVL.m} y
 * {@code ASIGNACORRELACIONESINFERIORESALVL.m} borran las entradas cuyo VALOR es
 * exactamente 1 ({@code ren(ren==1)=0}) en vez de las de la posición
 * {@code (i,i)}. Eso también borra los alias PERFECTOS: un
 * {@code 2^(3-1)} con {@code I=ABC} (donde {@code r(A,BC)=1}) sale reportado
 * como diseño ortogonal, sin ningún alias. Aquí se descarta la diagonal por
 * posición, de modo que ese diseño devuelve el resultado de libro
 * {@code A = A + BC}, {@code B = B + AC}, {@code C = C + AB}. Los cuatro
 * ejercicios verificados no contienen correlaciones de exactamente 1, así que
 * el cambio no altera ninguno de sus resultados.</p>
 *
 * <p><b>Memoria — diseños grandes:</b> el número de efectos crece como
 * {@code L = n + C(n,2) + C(n,3)}; para {@code n=40} factores, {@code L≈10 700}.
 * Todas las matrices intermedias (correlaciones, umbral, cadenas) son
 * TRIANGULARES por construcción — el efecto {@code i} sólo puede estar
 * correlacionado con efectos de índice {@code ≤ i} — así que aquí se
 * representan como {@code double[L][]} con la fila {@code i} de tamaño
 * {@code i+1}, nunca como {@code double[L][L]}. Eso reduce a la mitad cada
 * buffer intermedio (y evita reservar de entrada una matriz {@code L×L} densa
 * sólo para descartar su mitad superior, que es lo que hacía la versión
 * anterior). Sólo al final se "densifica" para conservar la forma
 * {@code double[L][L]} que espera {@link AliasStructure} (API pública, no se
 * tocó). Las referencias a los buffers intermedios se sueltan explícitamente
 * en cuanto dejan de hacer falta para que el recolector de basura los libere
 * cuanto antes en vez de esperar a que termine el método.</p>
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

    /** {@code ponderacion} de {@code GENERADORESTRUCTURAALIASITC.m}. */
    private static final double WEIGHT = 0.5;

    /** {@code W(yy,yyy)<0.0001 → 0} de {@code PASO4.m}. */
    private static final double ZERO_TOL = 0.0001;

    private final double[][] fraction;
    private final int n; // factores

    private String[] effectNames;
    private int mainEffects;  // me
    private int twoFactor;    // doble

    public AliasAnalyzer(double[][] fraction) {
        if (fraction == null || fraction.length == 0) {
            throw new IllegalArgumentException("La fracción no puede estar vacía");
        }
        this.fraction = fraction;
        this.n = fraction[0].length;
    }

    /**
     * Número total de efectos (principales + dobles + triples) que tendrá que
     * calcular esta fracción, sin llegar a construir ninguna matriz. Útil para
     * estimar de antemano el costo en memoria/tiempo antes de lanzar
     * {@link #generate()} sobre un diseño grande.
     */
    public static long effectCount(int factors) {
        long doble = combinations(factors, 2);
        long triple = factors > 2 ? combinations(factors, 3) : 0;
        return factors + doble + triple;
    }

    private static long combinations(int n, int k) {
        long result = 1;
        for (int i = 0; i < k; i++) {
            result = result * (n - i) / (i + 1);
        }
        return result;
    }

    /**
     * Genera la estructura de alias.
     *
     * @throws IllegalStateException si los efectos principales están fuertemente
     *         correlacionados (ver nota de revisión sobre el guard).
     */
    public AliasStructure generate() {
        EffectLabeler labeler = new EffectLabeler(n);
        this.effectNames = labeler.effectNames();
        this.mainEffects = n;
        this.twoFactor = n * (n - 1) / 2;

        // T = tril(matrizdecorrelaciones), triangular desde su origen: nunca se
        // reserva la mitad superior (siempre cero) de una matriz L×L completa.
        double[][] t = computeLowerTriangleCorrelations();
        sanitizeNaN(t);

        if (hasStrongMainEffectCorrelations(t)) {
            throw new IllegalStateException(
                    "La fracción contiene efectos principales que están fuertemente "
                    + "correlacionados. No se puede calcular la estructura de alias.");
        }

        double vl = threshold(t);                     // VL = Q * ponderacion
        double[][] w = magnitudes(t);                  // W = abs(T), residuos a 0

        double[][] ch = buildChainMatrix(w, vl);
        w = null; // ya no hace falta: liberar antes de densificar el resultado.

        double[][] msz = restoreSigns(ch, t);          // CAMBIODESIGNOS (triangular)
        ch = null;
        t = null;

        return new AliasStructure(toDense(msz), effectNames, mainEffects);
    }

    // ── Correlaciones ────────────────────────────────────────────────────

    private double[][] computeLowerTriangleCorrelations() {
        double[][] modelMatrix = new ModelMatrixBuilder(fraction).build();
        return Correlation.lowerTriangleCorrcoef(modelMatrix);
    }

    /**
     * Una columna del modelo perfectamente confundida (varianza 0 — p. ej. una
     * triple totalmente aliada) hace que {@code corrcoef} devuelva NaN. Una
     * correlación indefinida NO es un alias: se sanea a 0 para que no se cuele
     * en el umbral ni en la asignación.
     */
    private void sanitizeNaN(double[][] t) {
        for (double[] row : t) {
            for (int j = 0; j < row.length; j++) {
                if (Double.isNaN(row[j])) {
                    row[j] = 0.0;
                }
            }
        }
    }

    /** {@code t} es triangular: para {@code i<j}, el valor vive en {@code t[j][i]}. */
    private boolean hasStrongMainEffectCorrelations(double[][] t) {
        for (int i = 0; i < mainEffects; i++) {
            for (int j = i + 1; j < mainEffects; j++) {
                if (Math.abs(t[j][i]) >= STRONG_CORRELATION_THRESHOLD) {
                    return true;
                }
            }
        }
        return false;
    }

    // ── PASO 4: umbral VL y magnitudes ────────────────────────────────────

    /**
     * {@code Q = max(abs(T(T<1))); VL = Q*ponderacion}. La diagonal vale
     * exactamente 1 y por eso queda fuera; el umbral es entonces la mitad de la
     * correlación más fuerte del diseño.
     */
    private static double threshold(double[][] t) {
        double q = 0.0;
        for (double[] row : t) {
            for (double v : row) {
                if (v < 1.0) {
                    q = Math.max(q, Math.abs(v));
                }
            }
        }
        return q * WEIGHT;
    }

    private static double[][] magnitudes(double[][] t) {
        int l = t.length;
        double[][] w = new double[l][];
        for (int i = 0; i < l; i++) {
            double[] row = t[i];
            double[] wrow = new double[row.length];
            for (int j = 0; j < row.length; j++) {
                double v = Math.abs(row[j]);
                wrow[j] = v < ZERO_TOL ? 0.0 : v;
            }
            w[i] = wrow;
        }
        return w;
    }

    // ── PASO 5: construcción de las cadenas ──────────────────────────────

    /**
     * Devuelve {@code CH}: por cada efecto, a lo sumo una entrada (la cabeza de
     * su cadena) más los unos de la diagonal de todo efecto que encabece una.
     */
    private double[][] buildChainMatrix(double[][] w, double vl) {
        int l = w.length;

        // BUSCACORRELACIONESSUPERIORESALVL: sólo |r| >= VL, sin la diagonal.
        double[][] revW = new double[l][];
        for (int v = 0; v < l; v++) {
            double[] wrow = w[v];
            double[] rrow = new double[wrow.length];
            for (int j = 0; j < wrow.length; j++) {
                double value = wrow[j];
                rrow[j] = (v == j || value < vl) ? 0.0 : value;
            }
            revW[v] = rrow;
        }

        if (isAllZero(revW)) {
            // Diseño ortogonal: cada efecto sólo consigo mismo.
            double[][] ch = emptyTriangular(l);
            for (int i = 0; i < l; i++) {
                ch[i][i] = 1.0;
            }
            return ch;
        }

        double[][] d = locateAboveThreshold(revW);
        revW = null; // absorbido por locateAboveThreshold (copiado a `em`); ya no hace falta.
        if (everyRowHasEntry(d)) {
            return d;
        }
        return assignBelowThreshold(d, w);
    }

    /**
     * {@code LOCALIZACORRELACIONESSUPERIORESALVL.m}: cada renglón conserva sólo
     * su correlación máxima, buscándola primero entre los efectos principales,
     * luego entre las dobles y por último entre las triples. Al asignar un
     * renglón se anula su columna homónima.
     */
    private double[][] locateAboveThreshold(double[][] revW) {
        int l = revW.length;
        int me = mainEffects;
        int doble = twoFactor;
        double[][] em = copy(revW);

        // Clasificación previa (vecceros), calculada ANTES de tocar nada.
        int[] block = new int[l];
        for (int i = 0; i < l; i++) {
            double main = maxIn(em[i], 0, me);
            double two = maxIn(em[i], me, me + doble);
            double any = maxIn(em[i], 0, l);
            if (any == 0.0) {
                block[i] = 0;
            } else if (main != 0.0) {
                block[i] = 1;
            } else if (two != 0.0) {
                block[i] = 2;
            }
        }

        // 1) Renglones con correlación contra un efecto principal.
        for (int i = me; i < l; i++) {
            if (block[i] == 1 && keepOnlyMax(em, i, 0, me)) {
                clearColumn(em, i);
            }
        }
        // 2) Renglones sin principal pero con interacción doble.
        for (int i = me; i < l; i++) {
            if (block[i] == 2 && maxIn(em[i], me, me + doble) != 0.0
                    && keepOnlyMax(em, i, me, me + doble)) {
                clearColumn(em, i);
            }
        }
        // 3) Renglones que sólo se correlacionan con triples.
        for (int i = me + doble; i < l; i++) {
            if (maxIn(em[i], 0, me + doble) == 0.0 && maxIn(em[i], me + doble, l) != 0.0) {
                block[i] = 3;
            }
        }
        for (int i = me + doble; i < l; i++) {
            if (block[i] == 3 && maxIn(em[i], 0, l) != 0.0
                    && keepOnlyMax(em, i, me + doble, l)) {
                clearColumn(em, i);
            }
        }
        return em;
    }

    /**
     * {@code ASIGNACORRELACIONESINFERIORESALVL.m}: rescata a los efectos que se
     * quedaron sin cadena permitiéndoles usar su mejor correlación aunque no
     * llegue a VL, sin encadenarse a un efecto que ya es miembro de otra.
     */
    private double[][] assignBelowThreshold(double[][] d, double[][] w) {
        int l = d.length;
        int me = mainEffects;

        // UD: TODAS las correlaciones (sin filtrar por VL), sin la diagonal.
        double[][] ud = new double[l][];
        for (int i = 0; i < l; i++) {
            double[] wrow = w[i];
            double[] urow = new double[wrow.length];
            for (int j = 0; j < wrow.length; j++) {
                urow[j] = i == j ? 0.0 : wrow[j];
            }
            ud[i] = urow;
        }

        // MFL: el renglón vacío en D se sustituye por el de UD. Los efectos
        // principales nunca son miembros (emptyTriangular ya los deja en cero).
        double[][] mfl = emptyTriangular(l);
        for (int i = me; i < l; i++) {
            mfl[i] = maxIn(d[i], 0, l) == 0.0 ? ud[i].clone() : d[i].clone();
        }
        ud = null;

        // ML: un efecto que ya encabeza una cadena no puede además ser miembro.
        double[][] ml = emptyTriangular(l);
        for (int i = 0; i < l; i++) {
            if (!(i >= me && maxInColumn(d, i) != 0.0)) {
                ml[i] = mfl[i].clone();
            }
        }
        mfl = null;

        // MZ: propagación — no encadenarse a un efecto que ya es miembro.
        boolean[] assigned = new boolean[l];
        double[][] mz = new double[l][];
        for (int i = 0; i < l; i++) {
            mz[i] = ml[i].clone();
            assigned[i] = maxIn(d[i], 0, me) > 0.0;
        }
        ml = null;
        boolean[] chained = assigned.clone();
        for (int i = me; i < l; i++) {
            if (assigned[i]) {
                continue;
            }
            double[] mzRow = mz[i];
            for (int j = me; j < mzRow.length; j++) {
                if (mzRow[j] == 0.0) {
                    continue;
                }
                if (chained[j]) {
                    mzRow[j] = 0.0;
                } else {
                    chained[i] = true;
                }
            }
        }

        // MX: de lo que quede en el renglón, sólo la correlación más grande.
        double[][] mx = new double[l][];
        for (int i = 0; i < l; i++) {
            mx[i] = mz[i].clone();
            keepOnlyMax(mx, i, 0, l);
        }
        mz = null;

        // CH: se anula el renglón de todo efecto que encabece una cadena y se
        // le pone su 1 en la diagonal.
        double[][] ch = emptyTriangular(l);
        for (int i = 0; i < l; i++) {
            if (!(i >= me && maxInColumn(mx, i) != 0.0)) {
                ch[i] = mx[i].clone();
            }
        }
        mx = null;
        for (int i = 0; i < me; i++) {
            ch[i][i] = 1.0;
        }
        for (int i = me; i < l; i++) {
            if (maxInColumn(ch, i) != 0.0) {
                ch[i][i] = 1.0;
            }
        }
        // Último recurso de Matlab: un efecto que no quedó ni de cabeza ni de
        // miembro se muestra como cadena trivial "X = X".
        if (!everyRowHasEntry(ch)) {
            for (int i = me; i < l; i++) {
                if (maxIn(ch[i], 0, l) == 0.0) {
                    ch[i][i] = 1.0;
                }
            }
        }
        return ch;
    }

    /** {@code CAMBIODESIGNOS.m}: devuelve el signo original a las magnitudes de CH. */
    private static double[][] restoreSigns(double[][] ch, double[][] t) {
        int l = ch.length;
        double[][] msz = new double[l][];
        for (int i = 0; i < l; i++) {
            double[] chRow = ch[i];
            double[] tRow = t[i];
            double[] mszRow = new double[chRow.length];
            for (int j = 0; j < chRow.length; j++) {
                mszRow[j] = Math.abs(tRow[j]) == Math.abs(chRow[j]) ? tRow[j] : 0.0;
            }
            msz[i] = mszRow;
        }
        return msz;
    }

    // ── Utilidades ───────────────────────────────────────────────────────

    /** Matriz triangular vacía: fila {@code i} de tamaño {@code i+1}, en cero. */
    private static double[][] emptyTriangular(int l) {
        double[][] m = new double[l][];
        for (int i = 0; i < l; i++) {
            m[i] = new double[i + 1];
        }
        return m;
    }

    /** Convierte una matriz triangular ({@code fila i} de tamaño {@code i+1}) a densa L×L. */
    private static double[][] toDense(double[][] triangular) {
        int l = triangular.length;
        double[][] dense = new double[l][l];
        for (int i = 0; i < l; i++) {
            System.arraycopy(triangular[i], 0, dense[i], 0, triangular[i].length);
        }
        return dense;
    }

    /**
     * Deja en el renglón {@code row} únicamente el primer valor máximo del
     * bloque {@code [from, to)} (recortado al tamaño real, triangular, del
     * renglón) y pone a cero todo lo demás del renglón.
     *
     * @return {@code true} si ese máximo es distinto de cero (hubo asignación).
     */
    private static boolean keepOnlyMax(double[][] matrix, int row, int from, int to) {
        double[] values = matrix[row];
        int limit = Math.min(to, values.length);
        if (from >= limit) {
            return false; // el renglón (triangular) no llega a este bloque.
        }
        int best = from;
        for (int j = from; j < limit; j++) {
            if (values[j] > values[best]) {
                best = j;
            }
        }
        double max = values[best];
        double[] reduced = new double[values.length];
        if (max != 0.0) {
            reduced[best] = max;
        }
        matrix[row] = reduced;
        return max != 0.0;
    }

    /**
     * Sólo los renglones {@code i >= column} pueden tener esa columna (fila
     * {@code i} tiene tamaño {@code i+1}); los anteriores no la alcanzan.
     */
    private static void clearColumn(double[][] matrix, int column) {
        for (int i = column; i < matrix.length; i++) {
            double[] row = matrix[i];
            if (column < row.length) {
                row[column] = 0.0;
            }
        }
    }

    private static double maxIn(double[] row, int from, int to) {
        int limit = Math.min(to, row.length);
        double max = Double.NEGATIVE_INFINITY;
        for (int j = from; j < limit; j++) {
            max = Math.max(max, row[j]);
        }
        return max == Double.NEGATIVE_INFINITY ? 0.0 : max;
    }

    /** Todos los valores en estas matrices son magnitudes ≥ 0, así que 0.0 es una base segura. */
    private static double maxInColumn(double[][] matrix, int column) {
        double max = 0.0;
        for (int i = column; i < matrix.length; i++) {
            double[] row = matrix[i];
            if (column < row.length && row[column] > max) {
                max = row[column];
            }
        }
        return max;
    }

    private static boolean everyRowHasEntry(double[][] matrix) {
        for (double[] row : matrix) {
            boolean found = false;
            for (double v : row) {
                if (v > 0.0) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAllZero(double[][] matrix) {
        for (double[] row : matrix) {
            for (double v : row) {
                if (v != 0.0) {
                    return false;
                }
            }
        }
        return true;
    }

    private static double[][] copy(double[][] matrix) {
        double[][] out = new double[matrix.length][];
        for (int i = 0; i < matrix.length; i++) {
            out[i] = matrix[i].clone();
        }
        return out;
    }
}
