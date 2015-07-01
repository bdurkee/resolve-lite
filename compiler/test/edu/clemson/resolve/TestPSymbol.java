package edu.clemson.resolve;

import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.parser.Resolve;
import edu.clemson.resolve.parser.ResolveLexer;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpBuildingListener;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.proving.absyn.PSymbol.PSymbolBuilder;
import edu.clemson.resolve.typereasoning.TypeGraph;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.Test;
import org.rsrg.semantics.Quantification;

import java.io.IOException;
import java.io.StringReader;

import static junit.framework.TestCase.assertEquals;

public class TestPSymbol extends BaseTest {

    protected static final Quantification FORALL = Quantification.UNIVERSAL;
    protected static final Quantification EXISTS = Quantification.EXISTENTIAL;
    protected static final Quantification NONE = Quantification.NONE;

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

    @Test public void testIsVariable() {
        TypeGraph g = new TypeGraph();
        PExp result = parseMathAssertionExp(g, "a");
        assertEquals(true, result.isVariable());
        result = parseMathAssertionExp(g, "a(x)");
        assertEquals(false, result.isVariable());
        result = parseMathAssertionExp(g, "a(x,y)");
        assertEquals(false, result.isVariable());
    }

    protected static ParseTree getTree(String input) {
        try {
            ANTLRInputStream in = new ANTLRInputStream(new StringReader(input));
            ResolveLexer lexer = new ResolveLexer(in);
            TokenStream tokens = new CommonTokenStream(lexer);
            Resolve parser = new Resolve(tokens);

            //Todo: For some reason this doesn't seem to be catching atm.
            if ( parser.getNumberOfSyntaxErrors() > 0 ) {
                throw new IllegalArgumentException("input string: " + input
                        + " for PExp test contains syntax error");
            }
            return parser.mathAssertionExp();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
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
     * @param input The input to parse.
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
