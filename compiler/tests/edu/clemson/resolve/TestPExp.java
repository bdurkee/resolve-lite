package edu.clemson.resolve;

import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.parser.ResolveParser;
import edu.clemson.resolve.parser.ResolveLexer;
import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpBuildingListener;
import edu.clemson.resolve.proving.absyn.PSymbol;
import org.antlr.v4.runtime.CommonToken;
import org.jetbrains.annotations.NotNull;
import edu.clemson.resolve.semantics.DumbMathClssftnHandler;
import edu.clemson.resolve.semantics.MathInvalidClassification;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;

import static edu.clemson.resolve.semantics.Quantification.*;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TestPExp extends BaseTest {

    private final DumbMathClssftnHandler g = new DumbMathClssftnHandler();

    @Test
    public void testGetSubExpressions() throws Exception {
        PExp result = parseMathAssertionExp(g, "x + y");
        List<? extends PExp> subexprs = result.getSubExpressions();
        Assert.assertEquals(3, subexprs.size());
        Iterator<? extends PExp> exps = subexprs.iterator();
        Assert.assertEquals("+", exps.next().toString());
        Assert.assertEquals("x", exps.next().toString());
        Assert.assertEquals("y", exps.next().toString());

        result = parseMathAssertionExp(g, "x(z + 1) + y");
        exps = result.getSubExpressions().iterator();
        Assert.assertEquals(3, result.getSubExpressions().size());
        Assert.assertEquals("+", exps.next().toString());
        Assert.assertEquals("x((z + 1))", exps.next().toString());
        Assert.assertEquals("y", exps.next().toString());

        result = parseMathAssertionExp(g,
                "{{@x if true; @y if true and x; false otherwise;}}");
        exps = result.getSubExpressions().iterator();
        Assert.assertEquals(5, result.getSubExpressions().size());
        Assert.assertEquals("@x", exps.next().toString());
        Assert.assertEquals("true", exps.next().toString());
        Assert.assertEquals("@y", exps.next().toString());
        Assert.assertEquals("(true and x)", exps.next().toString());
        Assert.assertEquals("false", exps.next().toString());
    }

    @Test
    public void testPSymbolAndPApplyEquals() throws Exception {
        Assert.assertEquals(true, parseMathAssertionExp(g, "x")
                .equals(parseMathAssertionExp(g, "x")));
        Assert.assertEquals(true, parseMathAssertionExp(g, "1")
                .equals(parseMathAssertionExp(g, "1")));
        Assert.assertEquals(true, parseMathAssertionExp(g, "true")
                .equals(parseMathAssertionExp(g, "true")));
        Assert.assertEquals(true, parseMathAssertionExp(g, "1 + 2")
                .equals(parseMathAssertionExp(g, "1 + 2")));

        Assert.assertNotEquals(parseMathAssertionExp(g, "1 + 2"),
                parseMathAssertionExp(g, "2 + 1"));
        Assert.assertNotEquals(parseMathAssertionExp(g, "f(x, y)"),
                parseMathAssertionExp(g, "f(x, 1)"));

        Assert.assertEquals(parseMathAssertionExp(g, "x * (z + y)"),
                parseMathAssertionExp(g, "x * (z + y)"));
        Assert.assertEquals(parseMathAssertionExp(g, "x * (z + y)"),
                parseMathAssertionExp(g, "x * (z + y)"));
        Assert.assertEquals(parseMathAssertionExp(g, "+(x, y)"),
                parseMathAssertionExp(g, "x + y"));
        Assert.assertEquals(parseMathAssertionExp(g, "f(x)(p(x))"),
                parseMathAssertionExp(g, "f(x)(p(x))"));

        Assert.assertEquals(parseMathAssertionExp(g, "conc.s"),
                parseMathAssertionExp(g, "conc.s"));

        Assert.assertNotEquals(parseMathAssertionExp(g, "foo"),
                parseMathAssertionExp(g, "bar :: foo"));
        Assert.assertEquals(parseMathAssertionExp(g, "bar :: f.x"),
                parseMathAssertionExp(g, "bar :: f.x"));
        Assert.assertNotEquals(parseMathAssertionExp(g, "bar :: f.x"),
                parseMathAssertionExp(g, "bar :: f.y"));
        Assert.assertEquals(parseMathAssertionExp(g, "||S||"),
                parseMathAssertionExp(g, "||S||"));
    }

    @Test
    public void testPAltAndPLambdaEquals() throws Exception {
        Assert.assertEquals(true, parseMathAssertionExp(g,
                "{{a if b = (c and f); b otherwise;}}")
                .equals(parseMathAssertionExp(g,
                        "{{a if b = (c and f); b otherwise;}}")));

        Assert.assertEquals(true, parseMathAssertionExp(g,
                "{{λ j : Z,(true) if b = (c and f); b otherwise;}}")
                .equals(parseMathAssertionExp(g,
                        "{{λ j : Z,(true) if b = (c and f); b otherwise;}}")));
    }

    //TODO: PSet needs finishing first -- this will probably be awhile
    @Test
    public void testPSetEquals() throws Exception {
    }

    @Test
    public void testIsObviouslyTrue() throws Exception {
        Assert.assertEquals(false, parseMathAssertionExp(g,
                "x + y = y + x").isObviouslyTrue());
        Assert.assertEquals(true, parseMathAssertionExp(g,
                "true").isObviouslyTrue());
        Assert.assertEquals(false, parseMathAssertionExp(g,
                "false").isObviouslyTrue());
        Assert.assertEquals(true, parseMathAssertionExp(g,
                "x * 3 + 2 = x * 3 + 2").isObviouslyTrue());
        Assert.assertEquals(true, parseMathAssertionExp(g,
                "+(x, y) = x + y").isObviouslyTrue());
    }

    @Test
    public void testIsLiteralFalse() {
        Assert.assertEquals(true, parseMathAssertionExp(g,
                "false").isLiteralFalse());
    }

    @Test
    public void testIsEquality() throws Exception {
        Assert.assertEquals(true, parseMathAssertionExp(g,
                "y + x = y + x").isEquality());
        Assert.assertEquals(true, parseMathAssertionExp(g,
                "1 = y + x").isEquality());
        Assert.assertEquals(false, parseMathAssertionExp(g,
                "1 and y + x").isEquality());
    }

    @Test
    public void testQuantifierDistribution() {
        PExp result = parseMathAssertionExp(g, "Forall x : Z, x = y");
        Iterator<? extends PExp> exps = result.getSubExpressions().iterator();
        Assert.assertEquals(1, result.getQuantifiedVariables().size());
        Assert.assertEquals(false, result.isLiteral());
        Assert.assertEquals(3, result.getSubExpressions().size());
        Assert.assertEquals(NONE, result.getQuantification());
        Assert.assertEquals(NONE, exps.next().getQuantification());
        Assert.assertEquals(UNIVERSAL, exps.next().getQuantification());
        Assert.assertEquals(NONE, exps.next().getQuantification());
    }

    @Test
    public void testNestedQuantifierDistribution() {
        PExp result =
                parseMathAssertionExp(g, "Forall x, y : Z, Exists v : Z, "
                        + "Forall f : Entity * Entity -> B, f(x, v)");
        Assert.assertEquals(3, result.getSubExpressions().size());
        Assert.assertEquals(3, result.getQuantifiedVariables().size());
        Assert.assertEquals(UNIVERSAL, result.getQuantification());

        Iterator<? extends PExp> exps = result.getSubExpressions().iterator();
        Assert.assertEquals(UNIVERSAL, exps.next().getQuantification());
        Assert.assertEquals(UNIVERSAL, exps.next().getQuantification());
        Assert.assertEquals(EXISTENTIAL, exps.next().getQuantification());
    }

    @Test
    public void testPSymbolAndPApplyContainsName() {
        assertEquals(true, parseMathAssertionExp(g, "f").containsName("f"));
        PExp result = parseMathAssertionExp(g, "f(h(g(x)))");
        assertEquals(true, result.containsName("x"));
        assertEquals(true, result.containsName("h"));
        assertEquals(false, result.containsName("a"));
        assertEquals(true, result.containsName("f"));
        assertEquals(true, result.containsName("g"));
        assertEquals(false, result.containsName("z"));
    }

    @Test
    public void testPAltAndPLambdaContainsName() {
        Assert.assertEquals(true, parseMathAssertionExp(g,
                "{{a if b = (c and f); b otherwise;}}").containsName("c"));
        Assert.assertEquals(true, parseMathAssertionExp(g,
                "λ x : Z,{{x = r if j; false otherwise;}}").containsName("r"));
        Assert.assertEquals(true, parseMathAssertionExp(g,
                "λ v : Z,{{x = r if j; false otherwise;}}").containsName("v"));
    }

    @Test
    public void testIsVariable() {
        PExp result = parseMathAssertionExp(g, "a");
        assertEquals(true, result.isVariable());
        result = parseMathAssertionExp(g, "a(x)");
        assertEquals(false, result.isVariable());
        result = parseMathAssertionExp(g, "a(x,y)");
        assertEquals(false, result.isVariable());
    }

    @Test
    public void testSplitIntoConjuncts() {
        PExp result = parseMathAssertionExp(g,
                "x and y = 2 and P.Lab = λ q : Z,(true)");
        List<PExp> conjuncts = result.splitIntoConjuncts();
        //Assert.assertEquals(2, conjuncts.size());
        Iterator<? extends PExp> exps = conjuncts.iterator();
        Assert.assertEquals(exps.next().toString(), "x");
        Assert.assertEquals(exps.next().toString(), "(y = 2)");
        Assert.assertEquals(exps.next().toString(), "(P.Lab = λ q:Inv,true)");

        result = parseMathAssertionExp(g, "f(p and (q and z))");
        Assert.assertEquals(1, result.splitIntoConjuncts().size());
        result = parseMathAssertionExp(g, "f(p and q, a and b)");
        Assert.assertEquals(1, result.splitIntoConjuncts().size());
        result = parseMathAssertionExp(g, "x or y");
        Assert.assertEquals(1, result.splitIntoConjuncts().size());
        result = parseMathAssertionExp(g, "x");
        Assert.assertEquals(1, result.splitIntoConjuncts().size());
    }

    @Test
    public void testWithQuantifiersFlipped1() {
        PExp result = parseMathAssertionExp(g, "Forall x : Z, x = y");
        Iterator<? extends PExp> exps = result.getSubExpressions().iterator();
        Assert.assertEquals(NONE, exps.next().getQuantification());
        Assert.assertEquals(UNIVERSAL, exps.next().getQuantification());
        Assert.assertEquals(NONE, exps.next().getQuantification());

        result = result.withQuantifiersFlipped();
        exps = result.getSubExpressions().iterator();

        Assert.assertEquals(NONE, exps.next().getQuantification());
        Assert.assertEquals(EXISTENTIAL, exps.next().getQuantification());
        Assert.assertEquals(NONE, exps.next().getQuantification());
    }

    //nothing flipped.
    @Test
    public void testWithQuantifiersFlipped2() {
        PExp result = parseMathAssertionExp(g, "Forall x, y, z : Z, a = c");
        Iterator<? extends PExp> exps = result.getSubExpressions().iterator();

        Assert.assertEquals(NONE, exps.next().getQuantification()); //a = c
        Assert.assertEquals(NONE, exps.next().getQuantification()); //a
        Assert.assertEquals(NONE, exps.next().getQuantification()); //c

        result = result.withQuantifiersFlipped();
        exps = result.getSubExpressions().iterator();
        Assert.assertEquals(NONE, exps.next().getQuantification());
        Assert.assertEquals(NONE, exps.next().getQuantification());
        Assert.assertEquals(NONE, exps.next().getQuantification());
    }

    @Test
    public void testWithQuantifiersFlipped3() {
        PExp result =
                parseMathAssertionExp(g,
                        "Forall x, y : Z, x and (Exists x : N, x is_in S)");
        Iterator<? extends PExp> exps = result.getSubExpressions().iterator();
        exps = result.getSubExpressions().iterator();
        Assert.assertEquals(NONE, exps.next().getQuantification()); //x and (Exists x : N, x is_in S)
        Assert.assertEquals(UNIVERSAL, exps.next().getQuantification()); //x
        PApply is_in_application = (PApply) exps.next();
        Assert.assertEquals(NONE, is_in_application.getQuantification()); //(Exists x : N, x is_in S)
        Assert.assertEquals(EXISTENTIAL,
                is_in_application.getArguments().get(0).getQuantification()); //x
        Assert.assertEquals(NONE,
                is_in_application.getArguments().get(1).getQuantification()); //S

        result = result.withQuantifiersFlipped();
        exps = result.getSubExpressions().iterator();
        Assert.assertEquals(NONE, exps.next().getQuantification()); //x and (Exists x : N, x is_in S)
        Assert.assertEquals(EXISTENTIAL, exps.next().getQuantification()); //x
        is_in_application = (PApply) exps.next();
        Assert.assertEquals(NONE, is_in_application.getQuantification()); //(Exists x : N, x is_in S)
        Assert.assertEquals(UNIVERSAL,
                is_in_application.getArguments().get(0).getQuantification()); //x
        Assert.assertEquals(NONE,
                is_in_application.getArguments().get(1).getQuantification()); //S
    }

    @Test
    public void testWithIncomingSignsRemoved() {
        PExp result = parseMathAssertionExp(g, "F(@I, J, @S.Top, @f(x)(y))");
        Assert.assertEquals(false, result.isIncoming());
        Iterator<? extends PExp> exps = result.getSubExpressions().iterator();
        boolean[] expected = { false, true, false, true, true };
        for ( boolean anExpected1 : expected ) {
            Assert.assertEquals(anExpected1, exps.next().isIncoming());
        }
        result = result.withIncomingSignsErased();
        exps = result.getSubExpressions().iterator();
        for ( boolean anExpected : expected ) {
            Assert.assertEquals(false, exps.next().isIncoming());
        }
    }

    @Test
    public void testSelectorExpWithCall() {
        PExp result = parseMathAssertionExp(g, "P(z).Q.Lab(s)(Cen(k))");
        Assert.assertEquals(false, result.isIncoming());

    }

    @Test
    public void testGetIncomingVariables() {
        PExp result =
                parseMathAssertionExp(g,
                        "Forall x, y, z : Z, Exists u, v, w : N," +
                                "@g(@u) + (h(@z, @w, @f(@u))) + " +
                                "λ q : Z,{{@x if g(x); @b(@k) otherwise;}}");
        Set<String> incomingNames = result.getIncomingVariables().stream()
                .map(e -> ((PSymbol) e).getName()).collect(Collectors.toSet());
        Set<String> expectedNames =
                Arrays.asList("g", "u", "z", "f", "w", "x", "b", "k").stream()
                        .collect(Collectors.toSet());
        Assert.assertEquals(expectedNames.size(), incomingNames.size());
        Assert.assertEquals(true, incomingNames.containsAll(expectedNames));
    }

    @Test
    public void testGetQuantifiedVariables() {
        PExp result =
                parseMathAssertionExp(
                        g,
                        "Forall x, y, z : Z, Exists u, v : N," +
                                "Forall f, h : Z * Z -> B, "
                                + "g(@u) + (h(@z, @w, f(@u)))");
        Set<String> quantifiedNames = result.getQuantifiedVariables().stream()
                .map(e -> ((PSymbol) e).getName()).collect(Collectors.toSet());
        Set<String> expectedNames = Arrays.asList("u", "z", "f", "h").stream()
                .collect(Collectors.toSet());
        Assert.assertEquals(4, quantifiedNames.size());
        Assert.assertEquals(true, quantifiedNames.containsAll(expectedNames));
    }

    @Test
    public void testGetFunctionApplications() {
    }

    @Test
    public void testGetSymbolNames() {
        PExp result = parseMathAssertionExp(g, "x + y");
        Set<String> expectedNames = Arrays.asList("x", "+", "y").stream()
                .collect(Collectors.toSet());
        Set<String> foundNames = result.getSymbolNames();
        Assert.assertEquals(expectedNames.size(), foundNames.size());
        Assert.assertEquals(true, foundNames.containsAll(expectedNames));
    }

    @Test
    public void testGetSymbolNames2() {
        PExp result =
                parseMathAssertionExp(g, "(λ q:Inv,({{P.Labl(SCD(q)) " +
                        "if ((SCD(q) + 1) <= P.Length);Label.base_point otherwise;}}))");
        Set<String> expectedNames =
                Arrays.asList("q", "Label.base_point", "P.Length").stream()
                        .collect(Collectors.toSet());
        Set<String> foundNames = result.getSymbolNames(true, true);
        Assert.assertEquals(expectedNames.size(), foundNames.size());
        Assert.assertEquals(true, foundNames.containsAll(expectedNames));

        //now don't exclude applications or literals
        expectedNames =
                Arrays.asList("q", "Label.base_point", "P.Length", "P.Labl",
                        "SCD", "+", "<=", "1").stream()
                        .collect(Collectors.toSet());
        foundNames = result.getSymbolNames();
        Assert.assertEquals(expectedNames.size(), foundNames.size());
        Assert.assertEquals(true, foundNames.containsAll(expectedNames));

    }

    @Test
    public void testSubstituteOnSelector() {
        PExp result = parseMathAssertionExp(g, "conc.P.Lab(conc.P.Trmnl_Loc)")
                .substitute(parseMathAssertionExp(g, "conc.P.Lab"),
                        parseMathAssertionExp(g, "X"));
        Assert.assertEquals("X(conc.P.Trmnl_Loc)", result.toString());
    }

    @Test
    public void testSubstituteOnLambda() {
        PExp result = parseMathAssertionExp(g, "X = λq : Inv,{{@e if j = i; @e(q) otherwise;}}")
                .substitute(parseMathAssertionExp(g, "@e"),
                        parseMathAssertionExp(g, "Y"));
        Assert.assertEquals("(X = λ q:Inv,{{Y if (j = i);Y(q) otherwise;}})", result.toString());
    }

    @Test
    public void testSplitIntoSequents() {
        PExp e = parseMathAssertionExp(g, "(Post implies (Q and R))");
        List<PExp> partitions = e.splitIntoSequents();
        Assert.assertEquals(2, partitions.size());
        Assert.assertEquals("(Post implies Q)", partitions.get(0).toString());
        Assert.assertEquals("(Post implies R)", partitions.get(1).toString());

        e = parseMathAssertionExp(g, "(Pre and (Post implies (Q and R)))");
        partitions = e.splitIntoSequents();
        Assert.assertEquals(3, partitions.size());
        Assert.assertEquals("(true implies Pre)", partitions.get(0).toString());
        Assert.assertEquals("(Post implies Q)", partitions.get(1).toString());
        Assert.assertEquals("(Post implies R)", partitions.get(2).toString());

        e = parseMathAssertionExp(g, "(P implies (Pre and (Post implies (Q and R))))");
        partitions = e.splitIntoSequents();
        Assert.assertEquals(3, partitions.size());
        Assert.assertEquals("(P implies Pre)", partitions.get(0).toString());
        Assert.assertEquals("((P and Post) implies Q)", partitions.get(1).toString());
        Assert.assertEquals("((P and Post) implies R)", partitions.get(2).toString());

        e = parseMathAssertionExp(g, "(P implies (Q implies (R implies (T and true))))");
        partitions = e.splitIntoSequents();
        Assert.assertEquals(2, partitions.size());
        Assert.assertEquals("(((P and Q) and R) implies T)", partitions.get(0).toString());
        Assert.assertEquals("(((P and Q) and R) implies true)", partitions.get(1).toString());
    }

    protected static ParseTree getTree(String input) {
        try {
            ANTLRInputStream in = new ANTLRInputStream(new StringReader(input));
            ResolveLexer lexer = new ResolveLexer(in);
            TokenStream tokens = new CommonTokenStream(lexer);
            ResolveParser parser = new ResolveParser(tokens);

            //Todo: For some reason this never seems to be getting tripped atm,
            //even in the presence of errors.
            if ( parser.getNumberOfSyntaxErrors()>0 ) {
                throw new IllegalArgumentException("input string: " + input
                        + " for PExp returnEnsuresArgSubstitutions contains syntax error");
            }
            return parser.mathAssertionExp();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Constructs an (untyped) {@link PExp} from string {@code input}.
     *  In the interest of avoiding null pointer exceptions, if the AST builder
     *  fails and comes back with {@code null}, this function will always
     *  return a dummy {@code true} expr; never just {@code null}.
     *
     *  <p>Also: Building even moderately sized {@link PExp}s is a pain; building one
     *  with real type information is an even bigger pain. Thus, for test methods
     *  where this function is used, know that we don't care about types so much
     *  as we do about correct expression structure and quantifier
     *  distribution. So instead of real type information we typically just use
     *  {@link MathInvalidClassification}.</p>
     *
     *  <p>If you <em>want</em> to test something math type related, just
     *  construct smaller exprs manually using {@link PSymbol.PSymbolBuilder}
     *  or {@link PApply.PApplyBuilder}; otherwise parse the larger expr
     *  using this method.</p>
     *
     * @param input the input to parse
     * @return the dummy-typed {@link PExp} representation of {@code input}
     */
    @NotNull
    public static PExp parseMathAssertionExp(@NotNull DumbMathClssftnHandler g,
                                             @NotNull String input) {
        ParseTree t = getTree(input);
        AnnotatedModule fakeModule =
                new AnnotatedModule(t, new CommonToken(ResolveLexer.ID, "T"),
                        "T.resolve", false);
        PExpBuildingListener<PExp> l =
                new PExpBuildingListener<>(g, fakeModule, true);
        ParseTreeWalker.DEFAULT.walk(l, t);
        PExp result = l.getBuiltPExp(t);
        return result==null ? g.getTrueExp() : result;
    }
}
