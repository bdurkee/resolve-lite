package org.resolvelite;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.Test;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.compiler.tree.ResolveTokenFactory;
import org.resolvelite.parsing.ResolveLexer;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.proving.absyn.PExpBuildingListener;
import org.resolvelite.proving.absyn.PSymbol;
import org.resolvelite.semantics.Quantification;
import org.resolvelite.typereasoning.TypeGraph;

import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;

import static org.junit.Assert.*;

public class PExpTest {

    /**
     * For methods where an instance of {@link PSymbol.PSymbolBuilder} is used
     * to construct testable {@link PSymbol}s, math types will also be tested.
     */
    @Test public void testLiterals() throws Exception {
        TypeGraph g = new TypeGraph();
        PSymbol result =
                new PSymbol.PSymbolBuilder("0").mathType(g.Z).literal(true)
                        .build();
        assertEquals(0, result.getQuantifiedVariables().size());
        assertEquals(g.Z, result.getMathType());
        assertEquals(true, result.isLiteral());
        assertEquals(Quantification.NONE, result.getQuantification());
    }

    @Test public void testQuantifierDistribution() throws Exception {
        PExp result = parseMathAssertionExp("Forall x : Z, x = y");
        Iterator<? extends PExp> subexps =
                result.getSubExpressions().iterator();
        assertEquals(1, result.getQuantifiedVariables().size());
        assertEquals(false, result.isLiteral());
        assertEquals(2, result.getSubExpressions().size());
        assertEquals(Quantification.NONE,
                ((PSymbol) result).getQuantification());
        assertEquals(Quantification.UNIVERSAL,
                ((PSymbol) subexps.next()).getQuantification());
        assertEquals(Quantification.NONE,
                ((PSymbol) subexps.next()).getQuantification());
    }

    @Test public void testNestedQuantifierDistribution() throws Exception {
        PExp result =
                parseMathAssertionExp("Forall x, y : Z, Exists v : Z, "
                        + "Forall f : Entity * Entity -> B, f(x, y)");
        assertEquals(2, result.getSubExpressions().size());
        assertEquals(3, result.getQuantifiedVariables().size());
    }

    protected static ParseTree getTree(String input) {
        ParseTree result = null;
        try {
            ANTLRInputStream in = new ANTLRInputStream(new StringReader(input));
            ResolveLexer lexer = new ResolveLexer(in);
            ResolveTokenFactory factory = new ResolveTokenFactory(in);
            lexer.setTokenFactory(factory);
            TokenStream tokens = new CommonTokenStream(lexer);
            ResolveParser parser = new ResolveParser(tokens);
            if ( parser.getNumberOfSyntaxErrors() > 0 ) {
                throw new IllegalArgumentException("input string for PExp"
                        + " test contains syntax error");
            }
            parser.setTokenFactory(factory);
            result = parser.mathAssertionExp();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * Constructs an (untyped) {@link PExp} from string {@code input}.
     * <p>
     * Building even moderately sized {@link PExp}s is a pain; building them
     * with type information is an even bigger pain. Thus, for test methods
     * where this function is used, know that we don't care about types so much
     * as we do about correct exp structure and quantifier distribution.</p>
     * <p>
     * In other words, if you want to test something math type related, just
     * use the builder, otherwise parse the actual expression using
     * this method.</p>
     * @param input
     * @return
     */
    protected static PExp parseMathAssertionExp(String input) {
        ParseTree t = getTree(input);
        AnnotatedTree dummy = new AnnotatedTree(t, "test", null, false);
        PExpBuildingListener<PExp> l =
                new PExpBuildingListener<>(new ParseTreeProperty<>(), dummy);
        ParseTreeWalker.DEFAULT.walk(l, t);
        return l.getBuiltPExp(t);
    }

}
