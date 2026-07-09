package me.julionxn.nobaitc.doe.alias;

import me.julionxn.nobaitc.math.Combinatorics;
import me.julionxn.nobaitc.math.LinearAlgebra;
import me.julionxn.nobaitc.math.VectorOps;
import me.julionxn.nobaitc.stats.Correlation;

import java.util.Arrays;

/**
 * Calcula la estructura de alias de una fracción factorial.
 *
 * <p>Reemplaza a {@code AliasStructureGenerator}. El algoritmo numérico (pasos
 * 4 y 5, portados de {@code PASO4.m} / {@code PASO5.m}) se conserva <b>idéntico</b>
 * para no alterar resultados ya validados; lo que cambia es la organización:</p>
 * <ul>
 *   <li>La generación de etiquetas vive en {@link EffectLabeler}.</li>
 *   <li>La construcción de la matriz-modelo vive en {@link ModelMatrixBuilder}.</li>
 *   <li>Se eliminaron los {@code System.out.println} / {@code printMatrix} de
 *       depuración que ensuciaban la consola.</li>
 *   <li>Usa la capa {@code math}/{@code stats} en lugar del antiguo
 *       {@code MatlabFunctions}.</li>
 * </ul>
 *
 * <p><b>HALLAZGO DE REVISIÓN (guard inerte):</b> el mensaje de error habla de
 * efectos principales "fuertemente correlacionados (r&gt;0.5)", pero el umbral
 * real ({@link #STRONG_CORRELATION_THRESHOLD} = 1.5) es inalcanzable para una
 * correlación normalizada en [-1, 1]. Es decir, esta validación <i>nunca se
 * dispara</i> con datos reales. Se conserva el valor original (1.5) para no
 * cambiar el comportamiento validado, pero se expone como constante para que
 * pueda ajustarse con criterio estadístico (por ejemplo, comparándolo contra el
 * VL) si se confirma que debía ser un umbral efectivo.</p>
 */
public class AliasAnalyzer {

    /** Ver nota de revisión en el Javadoc de la clase: umbral actualmente inerte. */
    private static final double STRONG_CORRELATION_THRESHOLD = 1.5;

    private static final double DIAGONAL_TOL = 0.0001;

    private final double[][] fraction;
    private final int n;            // factores
    private final double ponderacion;

    // Estado del proceso
    private double[][] T;           // triangular inferior de la correlación
    private double vl;              // valor límite
    private double[][] w;           // |T| con redondeo a cero de valores pequeños
    private int a;                  // filas de W
    private int l;                  // columnas de W (total de efectos)
    private int mainEffects;        // efectos principales
    private int doubles;            // interacciones de 2 factores
    private int triples;            // interacciones de 3 factores

    private String[] effectNames;
    private String[][] letterMatrix;
    private double[][] msz;         // resultado

    public AliasAnalyzer(double[][] fraction) {
        this(fraction, 0.5);
    }

    public AliasAnalyzer(double[][] fraction, double ponderacion) {
        if (fraction == null || fraction.length == 0) {
            throw new IllegalArgumentException("La fracción no puede estar vacía");
        }
        this.fraction = fraction;
        this.n = fraction[0].length;
        this.ponderacion = ponderacion;
    }

    /**
     * Genera la estructura de alias.
     *
     * @throws IllegalStateException si los efectos principales están fuertemente
     *         correlacionados (ver nota de revisión sobre el guard).
     */
    public AliasStructure generate() {
        double[][] correlations = computeCorrelations();
        step4(correlations);

        if (hasStrongMainEffectCorrelations()) {
            throw new IllegalStateException(
                    "La fracción contiene efectos principales que están fuertemente "
                    + "correlacionados. No se puede calcular la estructura de alias.");
        }

        step5();
        return new AliasStructure(msz, effectNames, letterMatrix, mainEffects);
    }

    // ── Pasos 1-3: correlaciones ──────────────────────────────────────────

    private double[][] computeCorrelations() {
        EffectLabeler labeler = new EffectLabeler(n);
        this.effectNames = labeler.effectNames();
        this.letterMatrix = labeler.letterMatrix();

        double[][] modelMatrix = new ModelMatrixBuilder(fraction).build();
        return Correlation.corrcoef(modelMatrix);
    }

    // ── Paso 4 ────────────────────────────────────────────────────────────

    private void step4(double[][] correlations) {
        T = LinearAlgebra.tril(correlations);

        // HALLAZGO DE REVISIÓN: una columna del modelo perfectamente confundida
        // (constante, varianza 0 — p. ej. una triple totalmente aliada) hace que
        // corrcoef devuelva NaN. Antes ese NaN se propagaba hasta el mapa de alias
        // y aparecía como "NaN <efecto>". Lo saneamos a 0 aquí: una correlación
        // indefinida NO es un alias fuerte. Los diseños sin columnas constantes no
        // tienen NaN, por lo que su resultado no cambia.
        for (int i = 0; i < T.length; i++) {
            for (int j = 0; j < T[i].length; j++) {
                if (Double.isNaN(T[i][j])) {
                    T[i][j] = 0.0;
                }
            }
        }

        // Q = max(|T|) excluyendo la diagonal (=1). VL = Q * ponderación.
        double q = 0;
        for (double[] row : T) {
            for (double v : row) {
                double abs = Math.abs(v);
                if (abs < 1.0 - 1e-10 && abs > q) {
                    q = abs;
                }
            }
        }
        vl = q * ponderacion;

        // W = |T|, con redondeo a cero de valores muy pequeños.
        w = new double[T.length][T[0].length];
        for (int i = 0; i < T.length; i++) {
            for (int j = 0; j < T[i].length; j++) {
                double abs = Math.abs(T[i][j]);
                w[i][j] = (abs < DIAGONAL_TOL) ? 0.0 : abs;
            }
        }

        a = w.length;
        l = w[0].length;
        mainEffects = n;
        doubles = Combinatorics.nchoosek(n, 2);
        triples = (n > 2) ? Combinatorics.nchoosek(n, 3) : 0;
    }

    private boolean hasStrongMainEffectCorrelations() {
        for (int col = 0; col < mainEffects - 1; col++) {
            for (int row = col + 1; row < mainEffects; row++) {
                if (Math.abs(w[row][col]) >= STRONG_CORRELATION_THRESHOLD) {
                    return true;
                }
            }
        }
        return false;
    }

    // ── Paso 5 ────────────────────────────────────────────────────────────

    private void step5() {
        double[][] revW = correlationsAboveVl();
        int aliasRows = countRowsWithAlias(revW);

        if (aliasRows == 0) {
            // Diseño ortogonal: MSZ = revW con diagonal = 1
            for (int t = 0; t < l; t++) {
                revW[t][t] = 1.0;
            }
            msz = revW;
        } else {
            double[][] d = locateCorrelationsAboveVl(revW);
            int aliasRows2 = countRowsWithAlias(d);

            double[][] ch;
            if (aliasRows2 == l) {
                ch = d;
            } else {
                ch = assignCorrelationsBelowVl(d);
            }
            msz = restoreSigns(ch);
        }
    }

    /** revW: copia de W con ceros en la diagonal y en valores &lt; VL. */
    private double[][] correlationsAboveVl() {
        double[][] revW = new double[a][l];
        for (int v = 0; v < a; v++) {
            for (int i = 0; i < l; i++) {
                double val = w[v][i];
                if (Math.abs(val - 1.0) < DIAGONAL_TOL || val < vl) {
                    revW[v][i] = 0.0;
                } else {
                    revW[v][i] = val;
                }
            }
        }
        return revW;
    }

    /** Cuenta filas con al menos un valor &gt; 0. */
    private int countRowsWithAlias(double[][] matrix) {
        int sum = 0;
        for (double[] row : matrix) {
            for (double val : row) {
                if (val > 0) {
                    sum++;
                    break;
                }
            }
        }
        return sum;
    }

    /**
     * Asigna a cada efecto el alias más fuerte, por rango: principales → dobles
     * → triples.
     */
    private double[][] locateCorrelationsAboveVl(double[][] em) {
        int[] kind = new int[a]; // 0=ninguno, 1=principal, 2=doble, 3=triple

        for (int vv = 0; vv < a; vv++) {
            double inMain = VectorOps.maxInRange(em[vv], 0, mainEffects);
            double inDouble = VectorOps.maxInRange(em[vv], mainEffects, mainEffects + doubles);
            double anyMax = VectorOps.max(em[vv]);

            if (anyMax == 0) {
                kind[vv] = 0;
            } else if (inMain != 0) {
                kind[vv] = 1;
            } else if (inDouble != 0) {
                kind[vv] = 2;
            }
        }

        // Aliados con un efecto principal
        for (int fx = mainEffects; fx < a; fx++) {
            if (kind[fx] == 1) {
                double[] vctr = Arrays.copyOfRange(em[fx], 0, mainEffects);
                int h = VectorOps.argmax(vctr);
                double r = vctr[h];
                if (r != 0) {
                    keepOnlyMax(vctr, h);
                    System.arraycopy(vctr, 0, em[fx], 0, mainEffects);
                    for (int i = mainEffects; i < l; i++) {
                        em[fx][i] = 0.0;
                    }
                    for (int i = 0; i < a; i++) {
                        em[i][fx] = 0.0;
                    }
                }
            }
        }

        // Aliados con un doble
        for (int vx = mainEffects; vx < a; vx++) {
            if (kind[vx] == 2) {
                double[] doubleRange = Arrays.copyOfRange(em[vx], mainEffects, mainEffects + doubles);
                double vc = VectorOps.max(doubleRange);
                if (vc != 0) {
                    int hg = VectorOps.argmax(doubleRange);
                    double rg = doubleRange[hg];
                    if (rg != 0) {
                        keepOnlyMax(doubleRange, hg);
                        Arrays.fill(em[vx], 0.0);
                        System.arraycopy(doubleRange, 0, em[vx], mainEffects, doubles);
                        for (int i = 0; i < a; i++) {
                            em[i][vx] = 0.0;
                        }
                    }
                }
            }
        }

        // Aliados con un triple
        if (triples > 0) {
            for (int gk = mainEffects + doubles; gk < a; gk++) {
                double inLower = VectorOps.maxInRange(em[gk], 0, mainEffects + doubles);
                double inTriple = VectorOps.maxInRange(em[gk], mainEffects + doubles, l);
                if (inLower == 0 && inTriple != 0) {
                    kind[gk] = 3;
                }
            }
            for (int vxx = mainEffects + doubles; vxx < a; vxx++) {
                if (kind[vxx] == 3) {
                    double ir = VectorOps.max(em[vxx]);
                    if (ir != 0) {
                        double[] tripleRange = Arrays.copyOfRange(em[vxx], mainEffects + doubles, l);
                        int hgg = VectorOps.argmax(tripleRange);
                        double rgg = tripleRange[hgg];
                        if (rgg != 0) {
                            keepOnlyMax(tripleRange, hgg);
                            System.arraycopy(tripleRange, 0, em[vxx], mainEffects + doubles, triples);
                            for (int i = 0; i < a; i++) {
                                em[i][vxx] = 0.0;
                            }
                        }
                    }
                }
            }
        }
        return em;
    }

    /** Deja sólo el elemento {@code maxIdx}; el resto se pone a 0. */
    private void keepOnlyMax(double[] arr, int maxIdx) {
        double maxVal = arr[maxIdx];
        for (int c = 0; c < arr.length; c++) {
            if (c == maxIdx) {
                continue;
            }
            if (maxVal > arr[c] || Math.abs(maxVal - arr[c]) < DIAGONAL_TOL) {
                arr[c] = 0.0;
            }
        }
    }

    /** Asigna los efectos que no quedaron aliados en la localización. */
    private double[][] assignCorrelationsBelowVl(double[][] d) {
        // UD = W sin la diagonal
        double[][] ud = new double[a][l];
        for (int dd = 0; dd < a; dd++) {
            for (int i = 0; i < l; i++) {
                if (i == dd && Math.abs(w[dd][i] - 1.0) < DIAGONAL_TOL) {
                    ud[dd][i] = 0.0;
                } else {
                    ud[dd][i] = w[dd][i];
                }
            }
        }

        // MFL: filas con alias en D usan D; el resto usa UD
        double[][] mfl = new double[a][l];
        for (int f = 0; f < a; f++) {
            double maxRow = VectorOps.max(d[f]);
            if (f < mainEffects || maxRow == 0) {
                System.arraycopy(ud[f], 0, mfl[f], 0, l);
            } else {
                System.arraycopy(d[f], 0, mfl[f], 0, l);
            }
        }

        // ML: si la columna ff>=me tiene algún alias en D, limpiar esa fila
        double[][] ml = new double[a][l];
        for (int ff = 0; ff < a; ff++) {
            System.arraycopy(mfl[ff], 0, ml[ff], 0, l);
            if (ff >= mainEffects) {
                double maxCol = LinearAlgebra.maxCol(d, ff);
                if (maxCol != 0) {
                    Arrays.fill(ml[ff], 0.0);
                }
            }
        }

        // MZ + vecAyuda: marcar qué efectos ya tienen alias en ME
        double[][] mz = new double[a][l];
        int[] helper = new int[l];
        for (int ll = 0; ll < a; ll++) {
            System.arraycopy(ml[ll], 0, mz[ll], 0, l);
            boolean hasValue = false;
            for (int i = 0; i < mainEffects; i++) {
                if (d[ll][i] > 0) {
                    hasValue = true;
                    break;
                }
            }
            helper[ll] = hasValue ? 1 : 0;
        }

        for (int ee = mainEffects; ee < a; ee++) {
            if (helper[ee] == 0) {
                for (int uu = mainEffects; uu < l; uu++) {
                    if (mz[ee][uu] != 0) {
                        if (helper[uu] == 1) {
                            mz[ee][uu] = 0.0;
                        } else {
                            helper[ee] = 1;
                        }
                    }
                }
            }
        }

        // MX: por fila dejar sólo el alias con mayor correlación
        double[][] mx = new double[a][l];
        for (int fff = 0; fff < a; fff++) {
            double[] row = mz[fff].clone();
            double maxVal = VectorOps.max(row);
            if (maxVal != 0) {
                int maxIdx = VectorOps.argmax(row);
                keepOnlyMax(row, maxIdx);
            }
            System.arraycopy(row, 0, mx[fff], 0, l);
        }

        // CH: si la columna eee>=me ya tiene alias en MX, limpiar su fila
        double[][] ch = new double[l][l];
        for (int eee = 0; eee < l; eee++) {
            System.arraycopy(mx[eee], 0, ch[eee], 0, l);
            if (eee >= mainEffects) {
                double maxCol = LinearAlgebra.maxCol(mx, eee);
                if (maxCol != 0) {
                    Arrays.fill(ch[eee], 0.0);
                }
            }
        }

        // Diagonal = 1 para efectos principales
        for (int ss = 0; ss < mainEffects; ss++) {
            ch[ss][ss] = 1.0;
        }
        // Diagonal = 1 para interacciones que son cabeza de alias
        for (int sss = mainEffects; sss < l; sss++) {
            double maxCol = LinearAlgebra.maxCol(ch, sss);
            if (maxCol != 0) {
                ch[sss][sss] = 1.0;
            }
        }

        // Filas >=me que quedaron vacías → diagonal = 1
        int aliasCount = countRowsWithAlias(ch);
        if (aliasCount < l) {
            for (int yy = mainEffects; yy < l; yy++) {
                double maxRow = VectorOps.max(ch[yy]);
                if (maxRow == 0) {
                    ch[yy][yy] = 1.0;
                }
            }
        }
        return ch;
    }

    /** Restaura los signos originales de T en MSZ donde el |valor| coincide. */
    private double[][] restoreSigns(double[][] ch) {
        double[][] result = new double[a][l];
        for (int ppp = 0; ppp < a; ppp++) {
            for (int i = 0; i < l; i++) {
                double absT = Math.abs(T[ppp][i]);
                double absCh = Math.abs(ch[ppp][i]);
                result[ppp][i] = (Math.abs(absT - absCh) < DIAGONAL_TOL) ? T[ppp][i] : 0.0;
            }
        }
        return result;
    }
}
