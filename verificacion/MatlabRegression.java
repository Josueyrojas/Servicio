import me.julionxn.nobaitc.doe.alias.*;
import java.util.*;

/**
 * Regresión contra corridas de Matlab (archivo "Matrizes_error_para_revisar_con_sistemas.xlsx",
 * hoja "matriz 1", y "Reporte_Fracciones" hojas B2/C2).
 *
 * 1) Empates numéricos: |r(A,BCD)| = |r(AC,BCD)| = 0.204124 y |r(B,AC)| = |r(D,AC)| = 0.2.
 *    Matlab resuelve el empate con la PRIMERA columna (max devuelve la 1a ocurrencia);
 *    antes el ruido de punto flotante (~1e-16) decidía y salía "AC = AC -0.204124·BCD".
 * 2) Nomenclatura: Matlab no usa la letra I (el factor 9 es J).
 */
public class MatlabRegression {
    static int pass = 0, fail = 0;

    static void check(String name, boolean ok) {
        if (ok) pass++; else fail++;
        System.out.println((ok ? "PASS " : "FAIL ") + name);
    }

    public static void main(String[] args) {
        // Matriz 1: 20 corridas x 6 factores (niveles 2,2,2,2,3,4)
        double[][] m1 = {
            {1,1,1,2,1,4},{1,2,1,2,3,1},{1,1,1,1,1,4},{1,2,2,1,1,2},{1,1,2,1,3,4},
            {1,2,2,2,2,3},{1,1,2,2,1,3},{1,2,1,1,2,3},{1,1,1,1,2,1},{1,2,2,2,3,2},
            {2,1,2,2,2,2},{2,2,2,1,2,4},{2,1,2,1,3,3},{2,2,2,2,1,1},{2,1,1,2,2,2},
            {2,2,1,2,2,4},{2,1,2,1,1,1},{2,2,1,1,1,2},{2,1,1,2,3,1},{2,2,1,1,3,3}
        };
        AliasStructure s = new AliasAnalyzer(m1).generate();
        Map<String, List<AliasStructure.AliasPair>> map = s.getAliasMap();

        check("matriz 1: 'AC' ya no encabeza una cadena", !map.containsKey("AC"));
        check("matriz 1: A contiene +0.204124 BCD", coef(map, "A", "BCD") > 0.2041 && coef(map, "A", "BCD") < 0.2042);
        check("matriz 1: B contiene -0.200000 AC", Math.abs(coef(map, "B", "AC") + 0.2) < 1e-6);
        check("matriz 1: 'AC' no tiene cadena propia con BCD", coef(map, "AC", "BCD") == 0.0);
        check("matriz 1: 8 cadenas con alias (A,B,C,D,E,F,BC,ABC), como Matlab", countHeadsWithAlias(map) == 8);

        // Nomenclatura sin la letra I (9 factores)
        double[][] nine = new double[10][9];
        for (int i = 0; i < 10; i++) for (int j = 0; j < 9; j++) nine[i][j] = 1 + ((i * (j + 2) + j) % 2);
        String[] names = new AliasAnalyzer(nine).generate().getEfectos();
        List<String> list = Arrays.asList(names);
        check("9 factores: el factor 9 se llama J", list.contains("J") && list.contains("AJ") && list.contains("HJ"));
        check("9 factores: no existe ninguna etiqueta con I", list.stream().noneMatch(n -> n.contains("I")));

        System.out.println("\n== RESULT: " + pass + " passed, " + fail + " failed ==");
        if (fail > 0) System.exit(1);
    }

    static boolean hasTerm(Map<String, List<AliasStructure.AliasPair>> m, String head, String t) {
        return coef(m, head, t) != 0.0;
    }

    static double coef(Map<String, List<AliasStructure.AliasPair>> m, String head, String term) {
        List<AliasStructure.AliasPair> l = m.get(head);
        if (l == null) return 0.0;
        for (AliasStructure.AliasPair p : l) if (p.efecto.equals(term)) return p.coeficiente;
        return 0.0;
    }

    static int countHeadsWithAlias(Map<String, List<AliasStructure.AliasPair>> m) {
        int n = 0;
        for (List<AliasStructure.AliasPair> l : m.values()) if (l.size() > 1) n++;
        return n;
    }
}