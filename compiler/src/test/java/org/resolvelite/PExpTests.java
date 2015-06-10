package org.resolvelite;

import org.junit.Test;
import org.resolvelite.proving.absyn.PSymbol;
import org.resolvelite.semantics.Quantification;
import org.resolvelite.typereasoning.TypeGraph;

import static org.junit.Assert.*;

public class PExpTests extends BaseTest {

    @Test public void testLiterals() throws Exception {
        TypeGraph g = new TypeGraph();
        PSymbol result =
                new PSymbol.PSymbolBuilder("0").mathType(g.Z).literal(true)
                        .build();
        assertEquals(result.getQuantifiedVariables().size(), 0);
        assertEquals(result.getMathType(), g.Z);
        assertEquals(result.isLiteral(), true);
        assertEquals(result.getQuantification(), Quantification.NONE);
    }

    //To test this it would be best to build a concrete syntax tree with the
    //parser, then simply run our PExpBuildingListener on concrete node, testing
    //quantification from what is returned. This would be nice too because it
    //would be easy to form arbitrarily complicated expressions.
    //Todo: Figure out a way to run the parser on just a string. I.e. put a
    //method in BaseTest that does this and returns a ParseTree back.
    //Then run PExpBuildingListener on it.
    @Test public void testQuantifierDistribution() throws Exception {

    }

}
