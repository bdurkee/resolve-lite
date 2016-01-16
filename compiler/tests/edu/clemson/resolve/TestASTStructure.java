package edu.clemson.resolve;

import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpListener;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.rsrg.semantics.TypeGraph;

//TODO: returnEnsuresArgSubstitutions visitors for outfix style also something with fencepost accept

public class TestASTStructure extends BaseTest {

    private static class TestListener extends PExpListener {
        public String trace = "";
        @Override public void beginChildren(@NotNull PExp e) {
            trace += "<"+getClassStr(e)+":"+"begin>:"+e+"\n";
        }
        @Override public void endChildren(@NotNull PExp e) {
            trace += "<"+getClassStr(e)+":"+"end>:"+e+"\n";
        }
    }

    private static String getClassStr(PExp exp) {
        String className = exp.getClass().getSimpleName();
        if (!(exp instanceof PApply)) return className;
        return className+":"+
                (((PApply) exp).getDisplayStyle()).toString()
                        .toLowerCase()+className;
    }

    @Test public void testPSymbolStructure() {
        TypeGraph g = new TypeGraph();
        String[] expected = {
            "<PApply:infixPApply:begin>:(x + (1 * y))\n" +
            "<PSymbol:begin>:+\n" +
            "<PSymbol:end>:+\n" +
            "<PSymbol:begin>:x\n" +
            "<PSymbol:end>:x\n" +
            "<PApply:infixPApply:begin>:(1 * y)\n" +
            "<PSymbol:begin>:*\n" +
            "<PSymbol:end>:*\n" +
            "<PSymbol:begin>:1\n" +
            "<PSymbol:end>:1\n" +
            "<PSymbol:begin>:y\n" +
            "<PSymbol:end>:y\n" +
            "<PApply:infixPApply:end>:(1 * y)\n" +
            "<PApply:infixPApply:end>:(x + (1 * y))\n"
        };
        PExp tree = TestPExp.parseMathAssertionExp(g, "x + 1 * y");
        TestListener v = new TestListener();
        tree.accept(v);
        Assert.assertEquals(expected[0], v.trace);
    }

    /*@Test public void testPAltStructure() {
        TypeGraph g = new TypeGraph();
        String[] expected = {
            "<PAlternatives:begin>:{{@e if (q = @P.Trmnl_Loc);@P.Lab(q) otherwise;}}\n" +
            "<PSymbol:begin>:@e\n" +
            "<PSymbol:end>:@e\n" +
            "<PApply:infixPApply:begin>:(q = @P.Trmnl_Loc)\n" +
            "<PSymbol:begin>:=\n" +
            "<PSymbol:end>:=\n" +
            "<PSymbol:begin>:q\n" +
            "<PSymbol:end>:q\n" +
            "<PSelector:begin>:@P.Trmnl_Loc\n" +
            "<PSymbol:begin>:@P\n" +
            "<PSymbol:end>:@P\n" +
            "<PSymbol:begin>:Trmnl_Loc\n" +
            "<PSymbol:end>:Trmnl_Loc\n" +
            "<PSelector:end>:@P.Trmnl_Loc\n" +
            "<PApply:infixPApply:end>:(q = @P.Trmnl_Loc)\n" +
            "<PSelector:begin>:@P.Lab(q)\n" +
            "<PSymbol:begin>:@P\n" +
            "<PSymbol:end>:@P\n" +
            "<PApply:prefixPApply:begin>:Lab(q)\n" +
            "<PSymbol:begin>:Lab\n" +
            "<PSymbol:end>:Lab\n" +
            "<PSymbol:begin>:q\n" +
            "<PSymbol:end>:q\n" +
            "<PApply:prefixPApply:end>:Lab(q)\n" +
            "<PSelector:end>:@P.Lab(q)\n" +
            "<PAlternatives:end>:{{@e if (q = @P.Trmnl_Loc);@P.Lab(q) otherwise;}}\n"
        };
        PExp tree = TestPExp.parseMathAssertionExp(g,
                "{{@e if q = @P.Trmnl_Loc;@P.Lab(q) otherwise;}}");
        TestListener v = new TestListener();
        tree.accept(v);
        Assert.assertEquals(expected[0], v.trace);
    }*/

    @Test public void testPSetStructure() {

    }

    //Todo: more tests with mixture of: PLambda, PAlts, & PSets.
}
