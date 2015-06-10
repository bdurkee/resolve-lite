package org.resolvelite;

import org.junit.Test;
import org.resolvelite.proving.absyn.PSymbol;
import org.resolvelite.semantics.Quantification;
import org.resolvelite.typereasoning.TypeGraph;

import static org.junit.Assert.*;

public class PExpBuilderTest {

    @Test public void testLiterals() {
        TypeGraph g = new TypeGraph();
        PSymbol result =
                new PSymbol.PSymbolBuilder("0").mathType(g.Z).literal(true)
                        .build();
        assertEquals(result.getQuantifiedVariablesNoCache().size(), 0);
        assertEquals(result.getMathType(), g.Z);
        assertEquals(result.isLiteral(), true);
        assertEquals(result.getQuantification(), Quantification.NONE);
    }

}
