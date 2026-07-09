package me.julionxn.nobaitc.doe.alias;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Estructura de alias resultante del análisis: matriz de alias, nombres de
 * efectos, matriz de letras y un mapa de consulta.
 *
 * <p>API pública idéntica a la versión anterior ({@code getMatrizAlias},
 * {@code getEfectos}, {@code getMatrizLetras}, {@code getNumEfectosPrincipales},
 * {@code getAliasMap}, {@code isOrthogonal}, {@code getAliasCount}), pero sin
 * Lombok, con getters explícitos, para que el módulo de dominio compile y se
 * pruebe de forma independiente.</p>
 */
public class AliasStructure {

    private final double[][] matrizAlias;
    private final String[] efectos;
    private final String[][] matrizLetras;
    private final int numEfectosPrincipales;
    private final Map<String, List<AliasPair>> aliasMap;

    /**
     * @param msz           matriz de alias (A x L)
     * @param effectNames   nombres de los efectos (longitud L)
     * @param letterMatrix  matriz de etiquetas (A x L)
     * @param mainEffects   número de efectos principales
     */
    public AliasStructure(double[][] msz, String[] effectNames,
                          String[][] letterMatrix, int mainEffects) {
        this.matrizAlias = msz;
        this.efectos = effectNames;
        this.matrizLetras = letterMatrix;
        this.numEfectosPrincipales = mainEffects;
        this.aliasMap = new LinkedHashMap<>();
        buildAliasMap();
    }

    // ── Getters (API estable) ─────────────────────────────────────────────

    public double[][] getMatrizAlias()       { return matrizAlias; }
    public String[] getEfectos()             { return efectos; }
    public String[][] getMatrizLetras()      { return matrizLetras; }
    public int getNumEfectosPrincipales()    { return numEfectosPrincipales; }
    public Map<String, List<AliasPair>> getAliasMap() { return aliasMap; }

    private void buildAliasMap() {
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
            List<AliasPair> pairs = new ArrayList<>();
            for (int xx = 0; xx < a; xx++) {
                double val = matrizAlias[xx][x];
                if (val != 0) {
                    pairs.add(new AliasPair(val, matrizLetras[xx][x]));
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
     * Resumen compacto de la estructura de alias, una línea por efecto
     * PRINCIPAL (A, B, C, …). Cada línea tiene la forma
     * {@code "A = A  -0.4019·B-C"}. Es la representación que se muestra tanto en
     * la columna "Alias" de la tabla de resultados como en la pestaña de alias.
     */
    public String mainEffectsSummary() {
        int a = matrizAlias.length;
        List<String> lines = new ArrayList<>();
        for (int x = 0; x < numEfectosPrincipales; x++) {
            List<String> terms = new ArrayList<>();
            for (int xx = 0; xx < a; xx++) {
                double val = matrizAlias[xx][x];
                if (val == 0) {
                    continue;
                }
                String sign = val >= 0 ? "+" : "-";
                double abs = Math.abs(val);
                String label = matrizLetras[xx][x];
                terms.add(Math.abs(abs - 1.0) < 0.0001
                        ? sign + label
                        : String.format("%s%.4f·%s", sign, abs, label));
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
                    System.out.println(matrizLetras[xx][x]);
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
            return String.format("%+.4f %s", coeficiente, efecto);
        }
    }
}
