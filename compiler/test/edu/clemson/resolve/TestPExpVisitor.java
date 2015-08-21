package edu.clemson.resolve;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpListener;
import org.junit.Assert;
import org.junit.Test;
import org.rsrg.semantics.TypeGraph;

public class TestPExpVisitor extends BaseTest {

    private static class DemoListener extends PExpListener {
        public String trace = "";
        @Override public void beginChildren(PExp e) {
            trace += "<"+e.getClass().getSimpleName()+":"+"begin>:"+e.getText(true)+"\n";
        }
        @Override public void endChildren(PExp e) {
            trace += "<"+e.getClass().getSimpleName()+":"+"end>:"+e.getText(true)+"\n";
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
        DemoListener v = new DemoListener();
        tree.accept(v);
        Assert.assertEquals(expected[0], v.trace);
    }

    @Test public void testPAltStructure() {
        TypeGraph g = new TypeGraph();
        String[] expected = {
            "<PAlternatives:begin>:{{@e(q = @P.Trmnl_Loc); @P.Lab(q), otherwise}}\n"+
            "<PSymbol:begin>:@e\n"+
            "<PSymbol:end>:@e\n"+
            "<PSymbol:begin>:(q = @P.Trmnl_Loc)\n"+
            "<PSymbol:begin>:q\n"+
            "<PSymbol:end>:q\n"+
            "<PSymbol:begin>:@P.Trmnl_Loc\n"+
            "<PSymbol:end>:@P.Trmnl_Loc\n"+
            "<PSymbol:end>:(q = @P.Trmnl_Loc)\n"+
            "<PSymbol:begin>:@P.Lab(q)\n"+
            "<PSymbol:begin>:q\n"+
            "<PSymbol:end>:q\n"+
            "<PSymbol:end>:@P.Lab(q)\n"+
            "<PAlternatives:end>:{{@e(q = @P.Trmnl_Loc); @P.Lab(q), otherwise}}\n"
        };
        PExp tree = TestPExp.parseMathAssertionExp(g,
                "{{@e if q = @P.Trmnl_Loc;@P.Lab(q) otherwise;}}");
        DemoListener v = new DemoListener();
        tree.accept(v);
        Assert.assertEquals(expected[0], v.trace);
    }

    @Test public void testPSetStructure() {

    }

    //Todo: more tests with mixture of: PLambda, PAlts, & PSets.
}
