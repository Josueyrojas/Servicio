import me.julionxn.nobaitc.doe.design.Design;
import me.julionxn.nobaitc.doe.nonbpa.NonbpaGenerator;
import me.julionxn.nobaitc.doe.nonbpa.Fraction;
import me.julionxn.nobaitc.doe.alias.AliasAnalyzer;
import me.julionxn.nobaitc.doe.alias.AliasStructure;
import me.julionxn.nobaitc.math.*;
import me.julionxn.nobaitc.stats.Correlation;
import java.util.*;

public class SmokeTest {
    static int pass=0, fail=0;
    static void check(String name, boolean cond){
        System.out.println((cond?"  PASS":"  FAIL")+" - "+name);
        if(cond) pass++; else fail++;
    }
    public static void main(String[] args){
        System.out.println("== math layer ==");
        check("gcd(12,18)=6", Combinatorics.gcd(12,18)==6);
        check("lcm({2,3,4})=12", Combinatorics.lcm(new int[]{2,3,4})==12);
        check("nchoosek(5,2)=10", Combinatorics.nchoosek(5,2)==10);
        double[] v={3,1,4,1,5};
        check("max=5", VectorOps.max(v)==5);
        check("argmax=4", VectorOps.argmax(v)==4);
        check("sum=14", VectorOps.sum(v)==14);
        double[][] m={{2,1},{1,2}};
        double[][] inv=LinearAlgebra.inv(m);
        check("inv [[2,1],[1,2]] -> det-based", Math.abs(inv[0][0]-2.0/3)<1e-9 && Math.abs(inv[0][1]+1.0/3)<1e-9);
        boolean threw=false;
        try{ LinearAlgebra.inv(new double[][]{{1,2},{2,4}});}catch(SingularMatrixException e){threw=true;}
        check("singular -> SingularMatrixException", threw);
        double[][] tr=LinearAlgebra.transpose(new double[][]{{1,2,3},{4,5,6}});
        check("transpose shape 3x2", tr.length==3 && tr[0].length==2 && tr[2][1]==6);

        System.out.println("== stats layer ==");
        double[][] cm = Correlation.corrcoef(new double[][]{{1,2},{2,4},{3,6},{4,8}});
        check("corrcoef diagonal=1", Math.abs(cm[0][0]-1)<1e-9);
        check("corrcoef perfect corr ~1", Math.abs(cm[0][1]-1)<1e-9);

        System.out.println("== uniqueRandom ==");
        long[] s = UniqueRandom.sample(1, 100, 50);
        Set<Long> set = new HashSet<>(); for(long x:s) set.add(x);
        check("50 unique in [1,100]", set.size()==50);

        System.out.println("== doe domain: Design ==");
        Design d = new Design(new int[]{2,3});   // TR=6, LCM=6 -> valid
        check("Design{2,3} TR=6", d.totalRuns()==6);
        check("Design{2,3} valid (TR==LCM)", d.isValid());
        check("Design{2,2} invalid (TR4!=LCM2)", !new Design(new int[]{2,2}).isValid());
        check("params sfMin = max(gl,maxLevel)", d.parameters().sfMin()== Math.max(4,3));

        System.out.println("== doe domain: NonbpaGenerator ==");
        Design d2 = new Design(new int[]{2,3,4}); // TR=24 LCM=12 -> invalid actually
        Design dv = new Design(new int[]{3,4,5}); // TR=60 LCM=60 valid
        check("Design{3,4,5} valid", dv.isValid());
        NonbpaGenerator gen = new NonbpaGenerator();
        List<Fraction> fr = gen.generateCustom(dv, 8, Arrays.asList(1,2,3));
        check("generated 3 fractions", fr.size()==3);
        Fraction f0=fr.get(0);
        check("fraction matrix 8x3", f0.getFraction().length==8 && f0.getFraction()[0].length==3);
        check("levels within range", checkLevels(f0.getFraction(), new int[]{3,4,5}));
        check("gbm == sum(gbmVector)", Math.abs(f0.getGbm()-VectorOps.sum(f0.getGbmVector()))<1e-9);
        check("vifs length == factors", f0.getVifs().length==3);
        check("fractionData formatted non-empty", f0.getFractionData().contains("["));

        System.out.println("== doe alias: AliasAnalyzer (orthogonal full factorial) ==");
        // Full factorial 2x2 design as a fraction: rows = all combos, should be orthogonal
        double[][] full2x2 = {
            {1,1},{1,2},{2,1},{2,2}
        };
        AliasStructure as = new AliasAnalyzer(full2x2).generate();
        check("alias MSZ square", as.getMatrizAlias().length==as.getMatrizAlias()[0].length);
        check("alias effects A,B,A-B present", Arrays.asList(as.getEfectos()).contains("A") && Arrays.asList(as.getEfectos()).contains("A-B"));
        check("full 2x2 factorial reported orthogonal", as.isOrthogonal());
        System.out.println("  -> effects: "+Arrays.toString(as.getEfectos())+", orthogonal="+as.isOrthogonal()+", aliasCount="+as.getAliasCount());

        System.out.println("== doe alias: 3-factor full factorial 2x2x2 ==");
        double[][] full222 = new double[8][3];
        int r=0;
        for(int i1=1;i1<=2;i1++)for(int i2=1;i2<=2;i2++)for(int i3=1;i3<=2;i3++){full222[r][0]=i1;full222[r][1]=i2;full222[r][2]=i3;r++;}
        AliasStructure as2 = new AliasAnalyzer(full222).generate();
        check("2x2x2 alias MSZ square", as2.getMatrizAlias().length==as2.getMatrizAlias()[0].length);
        check("2x2x2 has triples (A-B-C)", Arrays.asList(as2.getEfectos()).contains("A-B-C"));
        check("2x2x2 full factorial orthogonal", as2.isOrthogonal());
        System.out.println("  -> effects: "+Arrays.toString(as2.getEfectos()));
        System.out.println("  -> aliasCount="+as2.getAliasCount()+" orthogonal="+as2.isOrthogonal());

        System.out.println("\n== RESULT: "+pass+" passed, "+fail+" failed ==");
        if(fail>0) System.exit(1);
    }
    static boolean checkLevels(double[][] m, int[] design){
        for(double[] row:m) for(int c=0;c<row.length;c++) if(row[c]<1||row[c]>design[c]) return false;
        return true;
    }
}
