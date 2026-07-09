package me.julionxn.nobaitc.ui.viewmodel;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import me.julionxn.nobaitc.doe.alias.AliasAnalyzer;
import me.julionxn.nobaitc.doe.alias.AliasStructure;
import me.julionxn.nobaitc.doe.nonbpa.Fraction;

/**
 * Adaptador de vista (view-model) que envuelve un {@link Fraction} de dominio y
 * expone propiedades JavaFX para la {@code TableView}.
 *
 * <p>Antes, el modelo {@code FractionResult} mezclaba datos de dominio con
 * propiedades de JavaFX. Esa separación es la que pedía la fase 3: el dominio
 * ({@link Fraction}) queda puro y testeable, y toda la dependencia de JavaFX
 * vive aquí, en la capa de UI.</p>
 *
 * <p>Los nombres de propiedad ({@code fractionNumber}, {@code startIndex},
 * {@code fractionData}, {@code gbm}, {@code gbmVectorData}, {@code j2},
 * {@code vifsData}) coinciden exactamente con los que usaba
 * {@code PropertyValueFactory} en {@code NONBPAController}, de modo que la tabla
 * sigue funcionando sin tocar el binding.</p>
 */
public class FractionRow {

    private final Fraction model;

    private final IntegerProperty fractionNumber;
    private final LongProperty startIndex;
    private final StringProperty fractionData;
    private final DoubleProperty gbm;
    private final StringProperty gbmVectorData;
    private final DoubleProperty j2;
    private final StringProperty vifsData;
    private final StringProperty aliasData;

    public FractionRow(Fraction model) {
        this.model = model;
        this.fractionNumber = new SimpleIntegerProperty(model.getFractionNumber());
        this.startIndex = new SimpleLongProperty(model.getStartIndex());
        this.fractionData = new SimpleStringProperty(model.getFractionData());
        this.gbm = new SimpleDoubleProperty(model.getGbm());
        this.gbmVectorData = new SimpleStringProperty(model.getGbmVectorData());
        this.j2 = new SimpleDoubleProperty(model.getJ2());
        this.vifsData = new SimpleStringProperty(model.getVifsData());
        this.aliasData = new SimpleStringProperty(computeAliasSummary(model));
    }

    /**
     * Calcula la estructura de alias de la fracción y la devuelve como resumen
     * por efectos principales. Si el cálculo falla (p. ej. efectos principales
     * fuertemente correlacionados), devuelve un texto breve en lugar de romper
     * la tabla.
     */
    private static String computeAliasSummary(Fraction model) {
        try {
            AliasStructure structure = new AliasAnalyzer(model.getFraction()).generate();
            if (structure.isOrthogonal()) {
                return "Ortogonal (sin alias)\n" + structure.mainEffectsSummary();
            }
            return structure.mainEffectsSummary();
        } catch (Exception e) {
            return "No disponible";
        }
    }

    /** Acceso al modelo de dominio subyacente (para escritores Excel/PDF, clipboard, alias). */
    public Fraction getModel() {
        return model;
    }

    // ── Propiedades JavaFX (requeridas por PropertyValueFactory) ───────────

    public IntegerProperty fractionNumberProperty() { return fractionNumber; }
    public LongProperty startIndexProperty()        { return startIndex; }
    public StringProperty fractionDataProperty()     { return fractionData; }
    public DoubleProperty gbmProperty()              { return gbm; }
    public StringProperty gbmVectorDataProperty()    { return gbmVectorData; }
    public DoubleProperty j2Property()               { return j2; }
    public StringProperty vifsDataProperty()         { return vifsData; }
    public StringProperty aliasDataProperty()        { return aliasData; }

    // ── Getters de conveniencia (delegan al modelo) ───────────────────────

    public int getFractionNumber()   { return model.getFractionNumber(); }
    public long getStartIndex()      { return model.getStartIndex(); }
    public double[][] getFraction()  { return model.getFraction(); }
    public double getGbm()           { return model.getGbm(); }
    public double[] getGbmVector()   { return model.getGbmVector(); }
    public double getJ2()            { return model.getJ2(); }
    public double[] getVifs()        { return model.getVifs(); }
    public String getFractionData()  { return model.getFractionData(); }
    public String getVifsData()      { return model.getVifsData(); }
    public String getGbmVectorData() { return model.getGbmVectorData(); }
    public String getAliasData()     { return aliasData.get(); }
}
