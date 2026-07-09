package me.julionxn.nobaitc.doe.nonbpa;

import me.julionxn.nobaitc.io.MatrixFormatter;

/**
 * Resultado de dominio <b>puro</b> de una fracción NONBPA: la matriz y sus
 * métricas. No depende de JavaFX.
 *
 * <p>Antes esto era {@code FractionResult}, que mezclaba el modelo de dominio
 * con propiedades de JavaFX ({@code SimpleIntegerProperty}, etc.). Esa parte de
 * vista se separó en {@code ui.viewmodel.FractionRow}. Así la lógica de negocio
 * es testeable y reutilizable (API, batch, tests) sin arrastrar la UI.</p>
 *
 * <p>Expone los mismos getters que consumían los escritores de Excel/PDF
 * ({@code getFraction}, {@code getFractionNumber}, {@code getGbm},
 * {@code getJ2}, {@code getVifs}, …) para que la migración sea directa.</p>
 */
public final class Fraction {

    private final int fractionNumber;   // consecutivo (1, 2, 3, …)
    private final long startIndex;      // número real de inicio (base 1)
    private final double[][] matrix;    // matriz de la fracción
    private final double gbm;           // GBM total (escalar)
    private final double[] gbmVector;   // GBM por factor
    private final double j2;            // parámetro J2
    private final double[] vifs;        // VIF por factor

    public Fraction(int fractionNumber, long startIndex, double[][] matrix,
                    double gbm, double[] gbmVector, double j2, double[] vifs) {
        this.fractionNumber = fractionNumber;
        this.startIndex = startIndex;
        this.matrix = matrix;
        this.gbm = gbm;
        this.gbmVector = gbmVector;
        this.j2 = j2;
        this.vifs = vifs;
    }

    public int getFractionNumber() { return fractionNumber; }
    public long getStartIndex()    { return startIndex; }
    public double[][] getFraction() { return matrix; }
    public double getGbm()         { return gbm; }
    public double[] getGbmVector() { return gbmVector; }
    public double getJ2()          { return j2; }
    public double[] getVifs()      { return vifs; }

    // ── Representaciones de texto (delegadas al formateador único) ─────────

    public String getFractionData()   { return MatrixFormatter.formatMatrix(matrix); }
    public String getVifsData()        { return MatrixFormatter.formatVerticalList(vifs); }
    public String getGbmVectorData()   { return MatrixFormatter.formatVerticalList(gbmVector); }
}
