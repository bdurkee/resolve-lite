package org.resolvelite;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.compiler.tree.ResolveTokenFactory;
import org.resolvelite.misc.Utils;
import org.resolvelite.parsing.ResolveLexer;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PSymbol;
import org.resolvelite.semantics.Quantification;
import org.resolvelite.typereasoning.TypeGraph;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.*;

public class PExpTest extends BaseTest {

    /**
     * For methods where an instance of {@link PSymbol.PSymbolBuilder} is used
     * to construct testable {@link PSymbol}s, math types will also be checked.
     * However, for methods where {@link #parseMathExp} is used, we don't care
     * about types so much as we do about expr structure and (correct)
     * quantifier distribution--so those are the things we're primarily checking
     * whenever that method is employed. In other words, if you want to test
     * something math type related, just use the builder, otherwise parse
     * the actual expression.
     */
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

    @Test public void testQuantifierDistribution() throws Exception {
        ParseTree x = parseMathExp("1 + 1");
        System.out.println("SDSDS: " + x.getText());
        int i;
        i = 0;
    }

    private ParseTree parseMathExp(String input) {
        ParseTree result = null;
        try {
            ANTLRInputStream in = new ANTLRInputStream(new StringReader(input));
            ResolveLexer lexer = new ResolveLexer(in);
            ResolveTokenFactory factory = new ResolveTokenFactory(in);
            lexer.setTokenFactory(factory);
            TokenStream tokens = new CommonTokenStream(lexer);
            ResolveParser parser = new ResolveParser(tokens);
            parser.setTokenFactory(factory);
            result = parser.mathExp();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

}
