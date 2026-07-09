import me.julionxn.nobaitc.doe.design.Design;
import me.julionxn.nobaitc.doe.nonbpa.*;
import me.julionxn.nobaitc.doe.alias.*;
import java.util.*;
public class EndToEnd {
    public static void main(String[] a){
        Design d = new Design(new int[]{3,4,5}); // TR=60, válido
        NonbpaGenerator gen = new NonbpaGenerator();
        // fracción real generada por el sistema
        List<Fraction> fr = gen.generateCustom(d, 12, Arrays.asList(1));
        double[][] frac = fr.get(0).getFraction();
        System.out.println("Fracción 12x3 generada (inicio=1). GBM="+fr.get(0).getGbm()
            +" J2="+fr.get(0).getJ2()+" VIFS="+Arrays.toString(fr.get(0).getVifs()));
        AliasStructure as = new AliasAnalyzer(frac).generate();
        System.out.println("Efectos ("+as.getEfectos().length+"): "+Arrays.toString(as.getEfectos()));
        System.out.println("Ortogonal: "+as.isOrthogonal()+"  | efectos con alias: "+as.getAliasCount());
        // verificar: sin NaN en el mapa, MSZ cuadrada
        boolean noNaN=true;
        for(double[] row: as.getMatrizAlias()) for(double v: row) if(Double.isNaN(v)) noNaN=false;
        System.out.println("MSZ sin NaN: "+noNaN+" | MSZ cuadrada: "+(as.getMatrizAlias().length==as.getMatrizAlias()[0].length));
        System.out.println(noNaN ? "PASS pipeline completo sin NaN" : "FAIL: NaN presente");
    }
}
