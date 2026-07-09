package me.julionxn.nobaitc.doe.alias;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Genera las etiquetas de los efectos (principales, dobles y triples) y la
 * matriz de letras asociada.
 *
 * <p>Extraído de {@code AliasStructureGenerator.generarCombinacionesLetras} y
 * {@code obtenerLetraExcel}. Responsabilidad única: nomenclatura de efectos.</p>
 */
final class EffectLabeler {

    private final String[] effectNames;     // renglonLetras
    private final String[][] letterMatrix;  // matrixLetras

    /**
     * @param factors número de factores (efectos principales)
     */
    EffectLabeler(int factors) {
        String[] variables = new String[factors];
        for (int i = 0; i < factors; i++) {
            variables[i] = excelLetter(i);
        }

        List<String> combinations = new ArrayList<>();
        // Efectos principales
        for (int i = 0; i < factors; i++) {
            combinations.add(variables[i]);
        }
        // Interacciones dobles
        for (int i = 0; i < factors; i++) {
            for (int j = i + 1; j < factors; j++) {
                combinations.add(variables[i] + "-" + variables[j]);
            }
        }
        // Interacciones triples
        if (factors > 2) {
            for (int i = 0; i < factors; i++) {
                for (int j = i + 1; j < factors; j++) {
                    for (int k = j + 1; k < factors; k++) {
                        combinations.add(variables[i] + "-" + variables[j] + "-" + variables[k]);
                    }
                }
            }
        }

        this.effectNames = combinations.toArray(new String[0]);
        int total = effectNames.length;
        this.letterMatrix = new String[total][total];
        for (int i = 0; i < total; i++) {
            Arrays.fill(letterMatrix[i], effectNames[i]);
        }
    }

    String[] effectNames() {
        return effectNames;
    }

    String[][] letterMatrix() {
        return letterMatrix;
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
