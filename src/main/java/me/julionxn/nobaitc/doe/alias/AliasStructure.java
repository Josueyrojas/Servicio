package me.julionxn.nobaitc.doe.alias;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Estructura de alias resultante del análisis: matriz de alias, nombres de
 * efectos y un mapa de consulta.
 *
 * <p>API pública ({@code getMatrizAlias}, {@code getEfectos},
 * {@code getNumEfectosPrincipales}, {@code getAliasMap}, {@code isOrthogonal},
 * {@code getAliasCount}), sin Lombok, con getters explícitos, para que el
 * módulo de dominio compile y se pruebe de forma independiente.</p>
 *
 * <p>{@code getMatrizLetras()} existía antes como una matriz {@code L×L} de
 * String donde CADA fila {@code xx} repetía {@code L} veces el mismo valor
 * {@code efectos[xx]} — nunca dependía de la segunda columna. Para diseños
 * grandes (p. ej. {@code L≈10 700} con 40 factores) esa redundancia por sí
 * sola pesaba casi 1&nbsp;GB en referencias. Se eliminó: donde antes se leía
 * {@code matrizLetras[xx][x]}, ahora se usa directamente {@code efectos[xx]}.</p>
 */
public class AliasStructure {

    private final double[][] matrizAlias;
    private final String[] efectos;
    private final int numEfectosPrincipales;
    private final Map<String, List<AliasPair>> aliasMap;

    /**
     * Tolerancia para tratar un coeficiente como "cero" al decidir si un efecto
     * tiene alias. Las correlaciones se acarrean desde divisiones en punto
     * flotante (p. ej. 1/3, 2/3 en la normalización de niveles) que a veces son
     * matemáticamente cero pero quedan como residuos de magnitud ~1e-16 tras la
     * comparación de signos en {@code AliasAnalyzer.restoreSigns}. Sin esta
     * tolerancia, esos residuos generaban términos fantasma como
     * "C = C  +0.0000·A-C" en lugar de "C = C".
     */
    private static final double ZERO_TOL = 1e-6;

    /**
     * @param msz           matriz de alias (A x L)
     * @param effectNames   nombres de los efectos (longitud L)
     * @param mainEffects   número de efectos principales
     */
    public AliasStructure(double[][] msz, String[] effectNames, int mainEffects) {
        this.matrizAlias = msz;
        this.efectos = effectNames;
        this.numEfectosPrincipales = mainEffects;
        this.aliasMap = new LinkedHashMap<>();
        buildAliasMap();
    }

    // ── Getters (API estable) ─────────────────────────────────────────────

    public double[][] getMatrizAlias()       { return matrizAlias; }
    public String[] getEfectos()             { return efectos; }
    public int getNumEfectosPrincipales()    { return numEfectosPrincipales; }
    public Map<String, List<AliasPair>> getAliasMap() { return aliasMap; }

    private void buildAliasMap() {
        int l = efectos.length;
        int a = matrizAlias.length;

        for (int x = 0; x < l; x++) {
            boolean hasAlias = false;
            for (int xx = 0; xx < a; xx++) {
                if (Math.abs(matrizAlias[xx][x]) > ZERO_TOL) {
                    hasAlias = true;
                    break;
                }
            }
            if (!hasAlias) {
                continue;
            }
            List<AliasPair> pairs = new ArrayList<>();
            for (int xx = 0; xx < a; xx++) {
                double val = matrizAlias[xx][x];
                if (Math.abs(val) > ZERO_TOL) {
                    pairs.add(new AliasPair(val, efectos[xx]));
                }
            }
            if (!pairs.isEmpty()) {
                aliasMap.put(efectos[x], pairs);
            }
        }
    }

    /** El diseño es ortogonal si cada efecto sólo está aliado consigo mismo. */
    public boolean isOrthogonal() {
        for (List<AliasPair> pairs : aliasMap.values()) {
            if (pairs.size() > 1) {
                return false;
            }
        }
        return true;
    }

    /** Número de efectos que tienen alias con otros efectos. */
    public int getAliasCount() {
        int count = 0;
        for (List<AliasPair> pairs : aliasMap.values()) {
            if (pairs.size() > 1) {
                count++;
            }
        }
        return count;
    }

    /**
     * Resumen compacto de la estructura de alias, una línea por cada efecto que
     * tiene contenido propio: los efectos PRINCIPALES (A, B, C, …) siempre
     * aparecen (aunque sea sin alias, p. ej. {@code "C = C"}), y además
     * cualquier interacción que sea "cabeza" de un alias (p. ej. una doble
     * aliada con otra doble: {@code "AB = AB  -0.9221·A-C"}). Antes el bucle se
     * limitaba a los {@code numEfectosPrincipales} primeros efectos, así que una
     * interacción aliada con otra interacción (sin que ningún efecto principal
     * participara) desaparecía por completo del resumen. Cada línea tiene la
     * forma {@code "A = A  -0.4019·B-C"}. Es la representación que se muestra
     * tanto en la columna "Alias" de la tabla de resultados como en la pestaña
     * de alias.
     */
    public String mainEffectsSummary() {
        int a = matrizAlias.length;
        List<String> lines = new ArrayList<>();
        for (int x = 0; x < efectos.length; x++) {
            List<String> terms = new ArrayList<>();
            for (int xx = 0; xx < a; xx++) {
                double val = matrizAlias[xx][x];
                if (Math.abs(val) <= ZERO_TOL) {
                    continue;
                }
                String sign = val >= 0 ? "+" : "-";
                double abs = Math.abs(val);
                String label = efectos[xx];
                terms.add(Math.abs(abs - 1.0) < 0.0001
                        ? sign + label
                        : String.format("%s%.6f·%s", sign, abs, label));
            }
            if (terms.isEmpty()) {
                continue;
            }
            StringBuilder line = new StringBuilder(efectos[x]).append(" = ");
            for (int t = 0; t < terms.size(); t++) {
                String term = terms.get(t);
                if (t == 0) {
                    line.append(term.startsWith("+") ? term.substring(1) : term);
                } else {
                    line.append("  ").append(term);
                }
            }
            lines.add(line.toString());
        }
        return String.join("\n", lines);
    }

    /** Imprime la estructura de alias por consola (utilidad opcional). */
    public void print() {
        System.out.println("\n============ ESTRUCTURA DE ALIAS ============");
        int l = efectos.length;
        int a = matrizAlias.length;
        for (int x = 0; x < l; x++) {
            boolean hasAlias = false;
            for (int xx = 0; xx < a; xx++) {
                if (matrizAlias[xx][x] != 0) {
                    hasAlias = true;
                    break;
                }
            }
            if (!hasAlias) {
                continue;
            }
            System.out.println("============");
            System.out.println("  EFECTO    ");
            System.out.println(efectos[x]);
            System.out.println("     =   ");
            for (int xx = 0; xx < a; xx++) {
                double val = matrizAlias[xx][x];
                if (val != 0) {
                    System.out.printf(" %+f %n", val);
                    System.out.println(efectos[xx]);
                }
            }
        }
        System.out.println("\n============================================");
    }

    /** Par de alias: coeficiente + nombre del efecto. */
    public static class AliasPair {
        public final double coeficiente;
        public final String efecto;

        public AliasPair(double coeficiente, String efecto) {
            this.coeficiente = coeficiente;
            this.efecto = efecto;
        }

        public double getCoeficiente() { return coeficiente; }
        public String getEfecto()      { return efecto; }

        @Override
        public String toString() {
            return String.format("%+.6f %s", coeficiente, efecto);
        }
    }
}
