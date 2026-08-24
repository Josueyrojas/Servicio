package me.julionxn.nobaitc.doe.alias;

import java.util.ArrayList;
import java.util.List;

/**
 * Genera las etiquetas de los efectos (principales, dobles y triples).
 *
 * <p>Extraído de {@code AliasStructureGenerator.generarCombinacionesLetras}.
 * Responsabilidad única: nomenclatura de efectos.</p>
 */
final class EffectLabeler {

    private final String[] effectNames;     // renglonLetras

    /**
     * @param factors número de factores (efectos principales)
     */
    EffectLabeler(int factors) {
        String[] variables = new String[factors];
        for (int i = 0; i < factors; i++) {
            variables[i] = excelLetter(i);
        }

        // Sin separador entre letras cuando cada factor cabe en una sola letra
        // (formato Matlab: 'AB', 'ACD', no "A-B") — así el resumen de alias
        // coincide con la salida de MATLAB de MATRIXLETRAS/RENGLONLETRAS. Pasados
        // los 26 factores, "AB"+"AC" (dobles de los factores 27 y 28) sería
        // indistinguible de "A"+"B"+"AC" (triple de los factores 1, 2 y 28): a
        // partir de ahí se usa "-" como separador para que cada nombre siga
        // siendo único.
        String sep = factors > 26 ? "-" : "";

        List<String> combinations = new ArrayList<>();
        // Efectos principales
        for (int i = 0; i < factors; i++) {
            combinations.add(variables[i]);
        }
        // Interacciones dobles
        for (int i = 0; i < factors; i++) {
            for (int j = i + 1; j < factors; j++) {
                combinations.add(variables[i] + sep + variables[j]);
            }
        }
        // Interacciones triples
        if (factors > 2) {
            for (int i = 0; i < factors; i++) {
                for (int j = i + 1; j < factors; j++) {
                    for (int k = j + 1; k < factors; k++) {
                        combinations.add(variables[i] + sep + variables[j] + sep + variables[k]);
                    }
                }
            }
        }

        this.effectNames = combinations.toArray(new String[0]);
    }

    String[] effectNames() {
        return effectNames;
    }

    /** Convierte un índice (0,1,…,26) en letras estilo Excel (A,B,…,AA). */
    private static String excelLetter(int index) {
        StringBuilder name = new StringBuilder();
        while (index >= 0) {
            name.insert(0, (char) ('A' + (index % 26)));
            index = (index / 26) - 1;
        }
        return name.toString();
    }
}
