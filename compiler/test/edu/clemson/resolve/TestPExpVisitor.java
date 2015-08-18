package edu.clemson.resolve;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpVisitor;
import edu.clemson.resolve.proving.absyn.PLambda;
import edu.clemson.resolve.proving.absyn.PSymbol;
import org.junit.Assert;
import org.junit.Test;
import org.rsrg.semantics.TypeGraph;

public class TestPExpVisitor extends BaseTest {

    public static class DemoVisitor extends PExpVisitor {
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
        DemoVisitor v = new DemoVisitor();
        tree.accept(v);
        Assert.assertEquals(expected[0], v.trace);
    }

    @Test public void testPAltStructure() {
        TypeGraph g = new TypeGraph();
        String[] expected = {
            "<PAlternatives:begin>:{@e if q = @P.Trmnl_Loc; @P.Lab(q), otherwise}\n"+
            "<PAlternatives:begin>:@e\n"+
            "<PAlternatives:end>:@e\n"+
            "<PAlternatives:begin>:(q = @P.Trmnl_Loc)\n"+
            "<PAlternatives:begin>:q\n"+
            "<PAlternatives:end>:q\n"+
            "<PAlternatives:begin>:@P.Trmnl_Loc\n"+
            "<PAlternatives:end>:@P.Trmnl_Loc\n"+
            "<PAlternatives:end>:(q = @P.Trmnl_Loc)\n"+
            "<PAlternatives:begin>:@P.Lab(q)\n"+
            "<PAlternatives:begin>:q\n"+
            "<PAlternatives:end>:q\n"+
            "<PAlternatives:end>:@P.Lab(q)\n"+
            "<PAlternatives:end>:{@e if q = @P.Trmnl_Loc; @P.Lab(q), otherwise}\n"
        };
        PExp tree = TestPExp.parseMathAssertionExp(g,
                "{{@e if q = @P.Trmnl_Loc;@P.Lab(q) otherwise;}}");
        DemoVisitor v = new DemoVisitor();
        tree.accept(v);
        System.out.println(v.trace);
        //Assert.assertEquals(expected[0], v.trace);
    }

    @Test public void testPSetStructure() {

    }

    //Todo: more tests with mixture of: PLambda, PAlts, & PSets.
}
