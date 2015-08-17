package edu.clemson.resolve;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpVisitor;
import edu.clemson.resolve.proving.absyn.PSymbol;
import org.junit.Assert;
import org.junit.Test;
import org.rsrg.semantics.TypeGraph;

public class TestPExpVisitor extends BaseTest {

    public static class DemoVisitor extends PExpVisitor {
        public String trace = "";
        @Override public void beginChildren(PExp e) {
            trace += "<"+e.getClass().getSimpleName()+":"+"begin>:"+e+"\n";
        }
        @Override public void endChildren(PExp e) {
            trace += "<"+e.getClass().getSimpleName()+":"+"end>:"+e+"\n";
        }
    }

    @Test public void testPSymbolStructure() {
        TypeGraph g = new TypeGraph();
        String[] expected = {
            "<PSymbol:begin>:(x + (1 * y))\n"+
            "<PSymbol:begin>:x\n"+
            "<PSymbol:end>:x\n"+
            "<PSymbol:begin>:(1 * y)\n"+
            "<PSymbol:begin>:1\n"+
            "<PSymbol:end>:1\n"+
            "<PSymbol:begin>:y\n"+
            "<PSymbol:end>:y\n"+
            "<PSymbol:end>:(1 * y)\n"+
            "<PSymbol:end>:(x + (1 * y))\n"
        };
        PExp tree = TestPExp.parseMathAssertionExp(g, "x + 1 * y");
        PExp e = TestPExp.parseMathAssertionExp(g, "(((1 <= Max_Depth) implies  ((|S| <= Max_Depth) implies  (Temp = Empty_String implies S = (Reverse(Temp) o S)))) and  ((1 <= Max_Depth) implies  ((|S| <= Max_Depth) implies  (S = (Reverse(Temp_p) o S_p) implies  (not((1 <= |S_p|)) implies  Temp_p = Reverse(S))))))");//and " +
        PExp spiral_text = TestPExp.parseMathAssertionExp(g, "P.Trmnl_Loc = SS(k)(0, @P.Trmnl_Loc) and P.Curr_Loc = @P.Curr_Loc and P.Lab = lambda (q : Sp_Loc(k)).({{@e if q = @P.Trmnl_Loc; @P.Lab(q) otherwise;}})");

        DemoVisitor v = new DemoVisitor();
        tree.accept(v);
        //Todo: Add @ incoming printing to PExpTextRenderingVisitor
        System.out.println(spiral_text.getText() + "\n" + spiral_text.toString());
        Assert.assertEquals(expected[0], v.trace);
    }

    //Todo: more tests with mixture of: PLambda, PAlts, & PSets.
}
