package edu.clemson.resolve;

import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpListener;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.rsrg.semantics.TypeGraph;

//TODO: test visitors for outfix style also something with fencepost accept

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
                (((PApply) exp).getDisplayStyle()).getStyleName()+className;
    }

    @Test public void testPSymbolStructure() {
        TypeGraph g = new TypeGraph();
        String[] expected = {
            "<PApply:InfixPApply:begin>:x + 1 * y\n" +
            "<PSymbol:begin>:+\n" +
            "<PSymbol:end>:+\n" +
            "<PSymbol:begin>:x\n" +
            "<PSymbol:end>:x\n" +
            "<PApply:InfixPApply:begin>:1 * y\n" +
            "<PSymbol:begin>:*\n" +
            "<PSymbol:end>:*\n" +
            "<PSymbol:begin>:1\n" +
            "<PSymbol:end>:1\n" +
            "<PSymbol:begin>:y\n" +
            "<PSymbol:end>:y\n" +
            "<PApply:InfixPApply:end>:1 * y\n" +
            "<PApply:InfixPApply:end>:x + 1 * y\n"
        };
        PExp tree = TestPExp.parseMathAssertionExp(g, "x + 1 * y");
        TestListener v = new TestListener();
        tree.accept(v);
        Assert.assertEquals(expected[0], v.trace);
    }

    @Test public void testPAltStructure() {
        TypeGraph g = new TypeGraph();
        String[] expected = {
            "<PAlternatives:begin>:{{@e if q = @P.Trmnl_Loc;@P.Lab(q) otherwise;}}\n" +
            "<PSymbol:begin>:@e\n" +
            "<PSymbol:end>:@e\n" +
            "<PApply:InfixPApply:begin>:q = @P.Trmnl_Loc\n" +
            "<PSymbol:begin>:=\n" +
            "<PSymbol:end>:=\n" +
            "<PSymbol:begin>:q\n" +
            "<PSymbol:end>:q\n" +
            "<PSymbol:begin>:@P.Trmnl_Loc\n" +
            "<PSymbol:end>:@P.Trmnl_Loc\n" +
            "<PApply:InfixPApply:end>:q = @P.Trmnl_Loc\n" +
            "<PApply:PrefixPApply:begin>:@P.Lab(q)\n" +
            "<PSymbol:begin>:@P.Lab\n" +
            "<PSymbol:end>:@P.Lab\n" +
            "<PSymbol:begin>:q\n" +
            "<PSymbol:end>:q\n" +
            "<PApply:PrefixPApply:end>:@P.Lab(q)\n" +
            "<PAlternatives:end>:{{@e if q = @P.Trmnl_Loc;@P.Lab(q) otherwise;}}\n"
        };
        PExp tree = TestPExp.parseMathAssertionExp(g,
                "{{@e if q = @P.Trmnl_Loc;@P.Lab(q) otherwise;}}");
        TestListener v = new TestListener();
        tree.accept(v);
        Assert.assertEquals(expected[0], v.trace);
    }

    @Test public void testPSetStructure() {

    }

    //Todo: more tests with mixture of: PLambda, PAlts, & PSets.
}
