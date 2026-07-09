package me.julionxn.nobaitc.doe.metrics;

import me.julionxn.nobaitc.doe.design.Design;
import me.julionxn.nobaitc.math.LinearAlgebra;
import me.julionxn.nobaitc.math.SingularMatrixException;
import me.julionxn.nobaitc.stats.Correlation;

/**
 * VIFS (Variance Inflation Factors): diagonal de la inversa de la matriz de
 * correlación de la fracción. Detecta multicolinealidad entre factores.
 *
 * <p>Equivale a la antigua clase {@code VIFSMatrix}. Métrica vectorial:
 * un VIF por factor.</p>
 */
public final class VifsMetric implements FractionMetric {

    @Override
    public String name() {
        return "VIFS";
    }

    @Override
    public double[] evaluate(double[][] fraction, Design design) {
        double[][] r2 = Correlation.corrcoef(fraction);
        try {
            double[][] r2Inverse = LinearAlgebra.inv(r2);
            return LinearAlgebra.diag(r2Inverse);
        } catch (SingularMatrixException e) {
            throw new IllegalStateException(
                    "No se logró calcular la inversa de la matriz de correlación (VIFS).", e);
        }
    }
}
