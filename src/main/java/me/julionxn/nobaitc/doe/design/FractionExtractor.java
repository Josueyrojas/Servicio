package me.julionxn.nobaitc.doe.design;

/**
 * Construye fracciones del diseño factorial mediante aritmética modular,
 * sin materializar nunca la matriz completa de TR×factores en memoria.
 *
 * <p>Es la <b>única</b> forma correcta de crear una fracción (patrón Factory):
 * ningún otro componente debe construir matrices de fracción "a mano".</p>
 *
 * <p>Reemplaza a {@code extractFractionOnTheFly} y elimina el código muerto
 * de matriz refleja ({@code buildReflexMatrix}, {@code createReflexMatrix},
 * {@code generateMainEffectsMatrix}, {@code fillFactorColumn}) que ya no se
 * usaba en {@code NONBPAGeneratorService}.</p>
 */
public final class FractionExtractor {

    private final Design design;
    private final long totalRuns;

    public FractionExtractor(Design design) {
        this.design = design;
        this.totalRuns = design.totalRuns();
    }

    /**
     * Extrae una fracción de {@code fractionSize} renglones comenzando en la
     * corrida {@code startRow} (índice base 0). El módulo (%) hace el papel de
     * la "matriz refleja", volviendo al inicio cuando se rebasa TR.
     *
     * @param startRow     fila inicial (base 0)
     * @param fractionSize número de renglones de la fracción
     * @return matriz {@code [fractionSize][factores]} con niveles en 1..nivel
     */
    public double[][] extract(long startRow, int fractionSize) {
        int factors = design.factors();
        int[] levels = design.levels();
        double[][] fraction = new double[fractionSize][factors];

        for (int r = 0; r < fractionSize; r++) {
            long actualRow = (startRow + r) % totalRuns;
            for (int c = 0; c < factors; c++) {
                fraction[r][c] = (actualRow % levels[c]) + 1;
            }
        }
        return fraction;
    }
}
