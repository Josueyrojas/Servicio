package me.julionxn.nobaitc.doe.nonbpa;

import me.julionxn.nobaitc.doe.design.Design;
import me.julionxn.nobaitc.doe.design.FractionExtractor;
import me.julionxn.nobaitc.doe.metrics.GbmMetric;
import me.julionxn.nobaitc.doe.metrics.J2Metric;
import me.julionxn.nobaitc.doe.metrics.VifsMetric;
import me.julionxn.nobaitc.math.UniqueRandom;

import java.util.ArrayList;
import java.util.List;

/**
 * Orquesta la generación de fracciones NONBPA: extrae cada fracción y le aplica
 * las métricas (GBM, J2, VIFS).
 *
 * <p>Reemplaza a {@code NONBPAGeneratorService}. Cambios principales:</p>
 * <ul>
 *   <li>Eliminado todo el código muerto de matriz refleja
 *       ({@code buildReflexMatrix}, {@code generateMainEffectsMatrix},
 *       {@code createReflexMatrix}, {@code fillFactorColumn},
 *       {@code extractFraction}).</li>
 *   <li>La validación vive ahora en {@link Design}.</li>
 *   <li>La extracción vive en {@link FractionExtractor} (Factory).</li>
 *   <li>Las métricas son estrategias intercambiables.</li>
 * </ul>
 */
public class NonbpaGenerator {

    private final GbmMetric gbm = new GbmMetric();
    private final J2Metric j2 = new J2Metric();
    private final VifsMetric vifs = new VifsMetric();

    /** Genera {@code numberOfFractions} fracciones con inicios aleatorios únicos. */
    public List<Fraction> generateRandom(Design design, int fractionSize, int numberOfFractions) {
        validateInputs(design, fractionSize, numberOfFractions);
        long tr = design.totalRuns();
        long[] starts = UniqueRandom.sample(1, tr, numberOfFractions);
        return buildFractions(design, fractionSize, starts);
    }

    /** Genera fracciones a partir de inicios definidos por el usuario. */
    public List<Fraction> generateCustom(Design design, int fractionSize, List<Integer> customStarts) {
        if (!design.isValid()) {
            throw new IllegalArgumentException("Diseño no válido para NONBPA");
        }
        if (!design.isFractionSizeValid(fractionSize)) {
            throw new IllegalArgumentException("Tamaño de fracción no válido");
        }
        long tr = design.totalRuns();
        validateCustomStarts(customStarts, tr);
        long[] starts = customStarts.stream().mapToLong(Integer::longValue).toArray();
        return buildFractions(design, fractionSize, starts);
    }

    // ── Núcleo ─────────────────────────────────────────────────────────────

    private List<Fraction> buildFractions(Design design, int fractionSize, long[] starts) {
        FractionExtractor extractor = new FractionExtractor(design);
        List<Fraction> results = new ArrayList<>(starts.length);

        for (int i = 0; i < starts.length; i++) {
            long startIndexZeroBased = starts[i] - 1;
            double[][] matrix = extractor.extract(startIndexZeroBased, fractionSize);

            double[] gbmVector = gbm.evaluate(matrix, design);
            double gbmTotal = 0;
            for (double v : gbmVector) {
                gbmTotal += v;
            }
            double j2Value = j2.scalar(matrix, design);
            double[] vifsVector = vifs.evaluate(matrix, design);

            // starts[i] es base-1 (igual que los números que ve el usuario)
            results.add(new Fraction(i + 1, starts[i], matrix,
                    gbmTotal, gbmVector, j2Value, vifsVector));
        }
        return results;
    }

    // ── Validaciones ─────────────────────────────────────────────────────

    private void validateInputs(Design design, int fractionSize, int numberOfFractions) {
        if (!design.isValid()) {
            throw new IllegalArgumentException("Diseño no válido para NONBPA");
        }
        if (!design.isFractionSizeValid(fractionSize)) {
            throw new IllegalArgumentException("Tamaño de fracción no válido");
        }
        long tr = design.totalRuns();
        if (numberOfFractions <= 0 || numberOfFractions > tr) {
            throw new IllegalArgumentException("Número de fracciones no válido (1-" + tr + ")");
        }
    }

    private void validateCustomStarts(List<Integer> customStarts, long maxValue) {
        for (int start : customStarts) {
            if (start < 1 || start > maxValue) {
                throw new IllegalArgumentException(
                        "Fracción " + start + " fuera del rango válido (1-" + maxValue + ")");
            }
        }
    }
}
