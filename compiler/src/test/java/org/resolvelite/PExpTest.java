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
import java.util.List;

import static org.junit.Assert.*;

public class PExpTest {

    protected static final Quantification FORALL = Quantification.UNIVERSAL;
    protected static final Quantification EXISTS = Quantification.EXISTENTIAL;
    protected static final Quantification NONE = Quantification.NONE;

    /**
     * For methods where an instance of {@link PSymbol.PSymbolBuilder} is used
     * to construct testable {@link PSymbol}s, math types will also be tested.
     */
    @Test public void testLiterals() {
        TypeGraph g = new TypeGraph();
        PSymbol result =
                new PSymbol.PSymbolBuilder("0").mathType(g.Z).literal(true)
                        .build();
        assertEquals(0, result.getQuantifiedVariables().size());
        assertEquals(g.Z, result.getMathType());
        assertEquals(true, result.isLiteral());
        assertEquals(Quantification.NONE, result.getQuantification());
    }

    @Test public void testQuantifierDistribution() {
        TypeGraph g = new TypeGraph();
        PExp result = parseMathAssertionExp(g, "Forall x : Z, x = y");
        Iterator<? extends PExp> exps = result.getSubExpressions().iterator();
        assertEquals(1, result.getQuantifiedVariables().size());
        assertEquals(false, result.isLiteral());
        assertEquals(2, result.getSubExpressions().size());
        assertEquals(NONE, ((PSymbol) result).getQuantification());
        assertEquals(FORALL, ((PSymbol) exps.next()).getQuantification());
        assertEquals(NONE, ((PSymbol) exps.next()).getQuantification());
    }

    @Test public void testNestedQuantifierDistribution() {
        TypeGraph g = new TypeGraph();
        PExp result =
                parseMathAssertionExp(g, "Forall x, y : Z, Exists v : Z, "
                        + "Forall f : Entity * Entity -> B, f(x, v)");
        assertEquals(2, result.getSubExpressions().size());
        assertEquals(3, result.getQuantifiedVariables().size());
        assertEquals(FORALL, ((PSymbol) result).getQuantification());

        Iterator<? extends PExp> exps = result.getSubExpressions().iterator();
        assertEquals(FORALL, ((PSymbol) exps.next()).getQuantification());
        assertEquals(EXISTS, ((PSymbol) exps.next()).getQuantification());
    }

    @Test public void testEquals() {
        TypeGraph g = new TypeGraph();

        PExp first = parseMathAssertionExp(g, "f(x,y,z+2)");
        PExp second = parseMathAssertionExp(g, "f(x,y,z+2)");
        assertEquals(first, second);
        second = parseMathAssertionExp(g, "f(x,y,z+1)");
        assertNotEquals(first, second);
        second = parseMathAssertionExp(g, "f(x,@y,z+2)");
        assertNotEquals(first, second);

        first = parseMathAssertionExp(g, "(x + y)");
        second = parseMathAssertionExp(g, "((x + y))");
        assertEquals(first, second);

        second = parseMathAssertionExp(g, "(((y) + x))");
        assertNotEquals(first, second);

        first = parseMathAssertionExp(g, "@x + @y");
        second = parseMathAssertionExp(g, "@x + @y");
        assertEquals(first, second);
        second = parseMathAssertionExp(g, "x + y");
        assertNotEquals(first, second);

        first = parseMathAssertionExp(g, "f(f(y)) + g(h + f(x))");
        second = parseMathAssertionExp(g, "(f(f(y)) + g(h + f(x)))");
        assertEquals(first, second);

        first = parseMathAssertionExp(g, "conc.G.S");
        second = parseMathAssertionExp(g, "conc.P.S");
        assertNotEquals(first, second);
        second = parseMathAssertionExp(g, "conc.G.S");
        assertEquals(first, second);
        second = parseMathAssertionExp(g, "@conc.P.S");
        assertNotEquals(first, second);
        second = parseMathAssertionExp(g, "conc.S.P");
        assertNotEquals(first, second);
        second = parseMathAssertionExp(g, "conc.S");
        assertNotEquals(first, second);
        second = parseMathAssertionExp(g, "conc");

        first = parseMathAssertionExp(g, "foo");
        second = parseMathAssertionExp(g, "bar::foo");
        assertNotEquals(first, second);
    }

    @Test public void testIsObviouslyTrue() {
        TypeGraph g = new TypeGraph();
        PExp result = parseMathAssertionExp(g, "f(x,y) = f(x,y)");
        assertEquals(true, result.isObviouslyTrue());
        result = parseMathAssertionExp(g, "f(x,y) = f(y,x)");
        assertEquals(false, result.isObviouslyTrue());

        result = parseMathAssertionExp(g, "25 = 25");
        assertEquals(true, result.isObviouslyTrue());

        result = parseMathAssertionExp(g, "true = false");
        assertEquals(false, result.isObviouslyTrue());
    }

    @Test public void testContainsName() {
        TypeGraph g = new TypeGraph();
        PExp result = parseMathAssertionExp(g, "f");
        assertEquals(true, result.containsName("f"));

        result = parseMathAssertionExp(g, "f(h(g(x)))");
        assertEquals(true, result.containsName("x"));
        assertEquals(true, result.containsName("h"));
        assertEquals(false, result.containsName("a"));
        assertEquals(true, result.containsName("f"));
        assertEquals(true, result.containsName("g"));
        assertEquals(false, result.containsName("z"));
    }

    @Test public void testVariable() {
        TypeGraph g = new TypeGraph();

        PExp result = parseMathAssertionExp(g, "a");
        assertEquals(true, result.isVariable());
        result = parseMathAssertionExp(g, "a(x)");
        assertEquals(false, result.isVariable());
        result = parseMathAssertionExp(g, "a(x,y)");
        assertEquals(false, result.isVariable());
    }

    @Test public void testLiteralFalse() {
        TypeGraph g = new TypeGraph();

        PExp result = parseMathAssertionExp(g, "false");
        assertEquals(true, result.isLiteralFalse());
    }

    @Test public void testSplitIntoConjuncts() {
        TypeGraph g = new TypeGraph();
        PExp result =
                parseMathAssertionExp(g, "x and y = 2 and "
                        + "P.Lab = lambda(q : Z).(true)");
        List<PExp> conjuncts = result.splitIntoConjuncts();
        assertEquals(3, conjuncts.size());
        Iterator<? extends PExp> exps = conjuncts.iterator();
        assertEquals(((PSymbol) exps.next()).getName(), "x");
        assertEquals(((PSymbol) exps.next()).getName(), "=");
        assertEquals(((PSymbol) exps.next()).getName(), "=");

        result = parseMathAssertionExp(g, "f(p and (q and z))");
        assertEquals(1, result.splitIntoConjuncts().size());
        result = parseMathAssertionExp(g, "f(p and q, a and b)");
        assertEquals(1, result.splitIntoConjuncts().size());
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

            //Todo: For some reason this doesn't seem to be catching atm.
            if ( parser.getNumberOfSyntaxErrors() > 0 ) {
                throw new IllegalArgumentException("input string: " + input
                        + " for PExp test contains syntax error");
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
     * Building even moderately sized {@link PExp}s is a pain; building one
     * with real type information is an even bigger pain. Thus, for test methods
     * where this function is used, know that we don't care about types so much
     * as we do about correct exp structure and quantifier distribution.</p>
     * <p>
     * In other words, if you want to test something math type related, just
     * construct smaller exps manually using {@link PSymbol.PSymbolBuilder},
     * otherwise parse the actual expression using this method.</p>
     *
     * @param input The string to parse.
     * @return The dummy-typed {@link PExp} representation.
     */
    protected static PExp parseMathAssertionExp(TypeGraph g, String input) {
        ParseTree t = getTree(input);
        AnnotatedTree dummy = new AnnotatedTree(t, "test", null, false);
        PExpBuildingListener<PExp> l =
                new PExpBuildingListener<>(new ParseTreeProperty<>(), dummy,
                        g.INVALID); //dummyType
        ParseTreeWalker.DEFAULT.walk(l, t);
        return l.getBuiltPExp(t);
    }

}
