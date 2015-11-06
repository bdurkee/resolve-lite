package edu.clemson.resolve;

import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.parser.ResolveLexer;
import edu.clemson.resolve.parser.ResolveParser;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpBuildingListener;
import edu.clemson.resolve.proving.absyn.PSymbol;
import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.TypeGraph;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.Assert;
import org.junit.Test;
import org.rsrg.semantics.Quantification;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TestPExp extends BaseTest {

    protected static final Quantification FORALL = Quantification.UNIVERSAL;
    protected static final Quantification EXISTS = Quantification.EXISTENTIAL;
    protected static final Quantification NONE = Quantification.NONE;
    private final TypeGraph g = new TypeGraph();

    @Test public void testGetSubExpressions() throws Exception {
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
        Assert.assertEquals("x(z + 1)", exps.next().toString());
        Assert.assertEquals("y", exps.next().toString());

        result = parseMathAssertionExp(g,
                "{{@x if true; @y if true and x; false otherwise;}}");
        exps = result.getSubExpressions().iterator();
        Assert.assertEquals(5, result.getSubExpressions().size());
        Assert.assertEquals("@x", exps.next().toString());
        Assert.assertEquals("true", exps.next().toString());
        Assert.assertEquals("@y", exps.next().toString());
        Assert.assertEquals("true and x", exps.next().toString());
        Assert.assertEquals("false", exps.next().toString());
    }

    @Test public void testPSymbolAndPApplyEquals() throws Exception {
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
    }

    @Test public void testPAltAndPLambdaEquals() throws Exception {
        Assert.assertEquals(true, parseMathAssertionExp(g,
                "{{a if b = (c and f); b otherwise;}}")
                    .equals(parseMathAssertionExp(g,
                            "{{a if b = (c and f); b otherwise;}}")));

        Assert.assertEquals(true, parseMathAssertionExp(g,
                "{{lambda (j : Z).(true) if b = (c and f); b otherwise;}}")
                    .equals(parseMathAssertionExp(g,
                            "{{lambda (j : Z).(true) if b = (c and f); b otherwise;}}")));
    }

    @Test public void testPSetEquals() throws Exception {
    }

    @Test public void testIsObviouslyTrue() throws Exception {
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

    @Test public void testIsEquality() throws Exception {
        Assert.assertEquals(true, parseMathAssertionExp(g,
                "y + x = y + x").isEquality());
        Assert.assertEquals(true, parseMathAssertionExp(g,
                "1 = y + x").isEquality());
        Assert.assertEquals(false, parseMathAssertionExp(g,
                "1 and y + x").isEquality());
    }

    /*@Test public void testQuantifierDistribution() {
        TypeGraph g = new TypeGraph();
        PExp result = parseMathAssertionExp(g, "Forall x : Z, x = y");
        Iterator<? extends PExp> exps = result.getSubExpressions().iterator();
        Assert.assertEquals(1, result.getQuantifiedVariables().size());
        Assert.assertEquals(false, result.isLiteral());
        Assert.assertEquals(2, result.getSubExpressions().size());
        Assert.assertEquals(NONE, ((PSymbol) result).getQuantification());
        Assert.assertEquals(FORALL, ((PSymbol) exps.next()).getQuantification());
        Assert.assertEquals(NONE, ((PSymbol) exps.next()).getQuantification());
    }

    @Test public void testNestedQuantifierDistribution() {
        TypeGraph g = new TypeGraph();
        PExp result =
                parseMathAssertionExp(g, "Forall x, y : Z, Exists v : Z, "
                        + "Forall f : Entity * Entity -> B, f(x, v)");
        Assert.assertEquals(2, result.getSubExpressions().size());
        Assert.assertEquals(3, result.getQuantifiedVariables().size());
        Assert.assertEquals(FORALL, ((PSymbol) result).getQuantification());

        Iterator<? extends PExp> exps = result.getSubExpressions().iterator();
        Assert.assertEquals(FORALL, ((PSymbol) exps.next()).getQuantification());
        Assert.assertEquals(EXISTS, ((PSymbol) exps.next()).getQuantification());
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

    @Test public void testIsVariable() {
        TypeGraph g = new TypeGraph();
        PExp result = parseMathAssertionExp(g, "a");
        assertEquals(true, result.isVariable());
        result = parseMathAssertionExp(g, "a(x)");
        assertEquals(false, result.isVariable());
        result = parseMathAssertionExp(g, "a(x,y)");
        assertEquals(false, result.isVariable());
    }

    @Test public void testEquals() {
        TypeGraph g = new TypeGraph();
        PExp first = parseMathAssertionExp(g, "f(x,y,z+2)");
        PExp second = parseMathAssertionExp(g, "f(x,y,z+2)");
        Assert.assertEquals(first, second);
        second = parseMathAssertionExp(g, "f(x,y,z+1)");
        assertNotEquals(first, second);
        second = parseMathAssertionExp(g, "f(x,@y,z+2)");
        assertNotEquals(first, second);

        first = parseMathAssertionExp(g, "(x + y)");
        second = parseMathAssertionExp(g, "((x + y))");
        Assert.assertEquals(first, second);

        second = parseMathAssertionExp(g, "(((y) + x))");
        assertNotEquals(first, second);

        first = parseMathAssertionExp(g, "@x + @y");
        second = parseMathAssertionExp(g, "@x + @y");
        Assert.assertEquals(first, second);
        second = parseMathAssertionExp(g, "x + y");
        assertNotEquals(first, second);

        first = parseMathAssertionExp(g, "f(f(y)) + g(h + f(x))");
        second = parseMathAssertionExp(g, "(f(f(y)) + g(h + f(x)))");
        Assert.assertEquals(first, second);

        first = parseMathAssertionExp(g, "conc.G.S");
        second = parseMathAssertionExp(g, "conc.P.S");
        assertNotEquals(first, second);
        second = parseMathAssertionExp(g, "conc.G.S");
        Assert.assertEquals(first, second);
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
        Assert.assertEquals(true, result.isObviouslyTrue());
        result = parseMathAssertionExp(g, "f(x,y) = f(y,x)");
        Assert.assertEquals(false, result.isObviouslyTrue());

        result = parseMathAssertionExp(g, "25 = 25");
        Assert.assertEquals(true, result.isObviouslyTrue());

        result = parseMathAssertionExp(g, "true = false");
        Assert.assertEquals(false, result.isObviouslyTrue());

        result = parseMathAssertionExp(g, "true = true");
        Assert.assertEquals(true, result.isObviouslyTrue());
    }

    @Test public void testIsLiteralFalse() {
        TypeGraph g = new TypeGraph();
        PExp result = parseMathAssertionExp(g, "false");
        Assert.assertEquals(true, result.isLiteralFalse());
    }

    @Test public void testSplitIntoConjuncts() {
        TypeGraph g = new TypeGraph();
        PExp result =
                parseMathAssertionExp(g, "x and y = 2 and "
                        + "P.Lab = lambda(q : Z).(true)");
        List<PExp> conjuncts = result.splitIntoConjuncts();
        Assert.assertEquals(3, conjuncts.size());
        Iterator<? extends PExp> exps = conjuncts.iterator();
        Assert.assertEquals(((PSymbol) exps.next()).getName(), "x");
        Assert.assertEquals(((PSymbol) exps.next()).getName(), "=");
        Assert.assertEquals(((PSymbol) exps.next()).getName(), "=");

        result = parseMathAssertionExp(g, "f(p and (q and z))");
        Assert.assertEquals(1, result.splitIntoConjuncts().size());
        result = parseMathAssertionExp(g, "f(p and q, a and b)");
        Assert.assertEquals(1, result.splitIntoConjuncts().size());
        result = parseMathAssertionExp(g, "x or y");
        Assert.assertEquals(1, result.splitIntoConjuncts().size());
        result = parseMathAssertionExp(g, "x");
        Assert.assertEquals(1, result.splitIntoConjuncts().size());
    }

    @Test public void testWithQuantifiersFlipped1() {
        TypeGraph g = new TypeGraph();
        PExp result = parseMathAssertionExp(g, "Forall x : Z, x = y");
        Iterator<? extends PExp> exps = result.getSubExpressions().iterator();
        Assert.assertEquals(FORALL, ((PSymbol) exps.next()).getQuantification());
        Assert.assertEquals(NONE, ((PSymbol) exps.next()).getQuantification());

        //now flip the quantifiers
        result = result.withQuantifiersFlipped();
        exps = result.getSubExpressions().iterator();

        Assert.assertEquals(EXISTS, ((PSymbol) exps.next()).getQuantification());
        Assert.assertEquals(NONE, ((PSymbol) exps.next()).getQuantification());
    }

    //nothing flipped.
    @Test public void testWithQuantifiersFlipped2() {
        TypeGraph g = new TypeGraph();
        PExp result = parseMathAssertionExp(g, "Forall x, y, z : Z, a = c");
        Iterator<? extends PExp> exps = result.getSubExpressions().iterator();

        Assert.assertEquals(NONE, ((PSymbol) exps.next()).getQuantification());
        Assert.assertEquals(NONE, ((PSymbol) exps.next()).getQuantification());
        result = result.withQuantifiersFlipped();
        exps = result.getSubExpressions().iterator();
        Assert.assertEquals(NONE, ((PSymbol) exps.next()).getQuantification());
        Assert.assertEquals(NONE, ((PSymbol) exps.next()).getQuantification());
    }

    //alternating flips
    @Test public void testWithQuantifiersFlipped3() {
        TypeGraph g = new TypeGraph();
        PExp result =
                parseMathAssertionExp(
                        g,
                        "Forall x, y, z : Z,"
                                + "Exists u, v, w : N, Forall f : Entity * Entity -> B,"
                                + "g(u, a(f(y)), z, w, f(u))");
        Iterator<? extends PExp> exps = result.getSubExpressions().iterator();
        exps = result.getSubExpressions().iterator();
        Assert.assertEquals(EXISTS, ((PSymbol) exps.next()).getQuantification()); //u

        //a(f(y))
        PSymbol aApp = (PSymbol) exps.next();
        PSymbol aAppArg = ((PSymbol) aApp.getArguments().get(0));

        Assert.assertEquals(NONE, (aApp).getQuantification());
        Assert.assertEquals(FORALL, aAppArg.getQuantification());
        Assert.assertEquals(FORALL,
                ((PSymbol) aAppArg.getArguments().get(0)).getQuantification());

        Assert.assertEquals(FORALL, ((PSymbol) exps.next()).getQuantification()); //z
        Assert.assertEquals(EXISTS, ((PSymbol) exps.next()).getQuantification()); //w

        //f(u)
        PSymbol fApp = (PSymbol) exps.next();
        Assert.assertEquals(FORALL, fApp.getQuantification());
        Assert.assertEquals(EXISTS,
                ((PSymbol) fApp.getArguments().get(0)).getQuantification());

        //Now flip em
        result = result.withQuantifiersFlipped();
        exps = result.getSubExpressions().iterator();
        Assert.assertEquals(FORALL, ((PSymbol) exps.next()).getQuantification()); //u

        //a(f(y))
        aApp = (PSymbol) exps.next();
        aAppArg = ((PSymbol) aApp.getArguments().get(0));

        Assert.assertEquals(NONE, (aApp).getQuantification());
        Assert.assertEquals(EXISTS, aAppArg.getQuantification());
        Assert.assertEquals(EXISTS,
                ((PSymbol) aAppArg.getArguments().get(0)).getQuantification());

        Assert.assertEquals(EXISTS, ((PSymbol) exps.next()).getQuantification()); //z
        Assert.assertEquals(FORALL, ((PSymbol) exps.next()).getQuantification()); //w

        //f(u)
        fApp = (PSymbol) exps.next();
        Assert.assertEquals(EXISTS, fApp.getQuantification());
        Assert.assertEquals(FORALL,
                ((PSymbol) fApp.getArguments().get(0)).getQuantification());
    }

    @Test public void testWithQuantifiersFlipped4() {
        TypeGraph g = new TypeGraph();
        PExp result =
                parseMathAssertionExp(g,
                        "Forall x, y : Z, x and (Exists x : N, x is_in S)");
        Iterator<? extends PExp> exps = result.getSubExpressions().iterator();
        exps = result.getSubExpressions().iterator();
        Assert.assertEquals(FORALL, ((PSymbol) exps.next()).getQuantification()); //x
        PSymbol app = (PSymbol) exps.next();
        Assert.assertEquals(NONE, app.getQuantification()); //x is_in S
        Assert.assertEquals(EXISTS,
                ((PSymbol) app.getArguments().get(0)).getQuantification()); //x
        Assert.assertEquals(NONE,
                ((PSymbol) app.getArguments().get(1)).getQuantification()); //S

        result = result.withQuantifiersFlipped();
        exps = result.getSubExpressions().iterator();
        Assert.assertEquals(EXISTS, ((PSymbol) exps.next()).getQuantification()); //x
        app = (PSymbol) exps.next();
        Assert.assertEquals(NONE, app.getQuantification()); //x is_in S
        Assert.assertEquals(FORALL,
                ((PSymbol) app.getArguments().get(0)).getQuantification()); //x
        Assert.assertEquals(NONE,
                ((PSymbol) app.getArguments().get(1)).getQuantification()); //S
    }

    @Test public void testWithIncomingSignsRemoved() {
        TypeGraph g = new TypeGraph();
        PExp result = parseMathAssertionExp(g, "F(@I, J, I, @S.Top)");
        Assert.assertEquals(false, ((PSymbol) result).isIncoming());
        Iterator<? extends PExp> exps = result.getSubExpressions().iterator();
        Assert.assertEquals(true, ((PSymbol) exps.next()).isIncoming());
        Assert.assertEquals(false, ((PSymbol) exps.next()).isIncoming());
        Assert.assertEquals(false, ((PSymbol) exps.next()).isIncoming());
    }

    @Test public void testGetIncomingVariables() {
        TypeGraph g = new TypeGraph();
        PExp result =
                parseMathAssertionExp( g,
                        "Forall x, y, z : Z, Exists u, v, w : N," +
                                "@g(@u) + (h(@z, @w, @f(@u))) + " +
                        "lambda (q : Z).({{@x if g(x); @b(@k) otherwise;}})");
        Set<String> incomingNames = result.getIncomingVariables().stream()
                .map(e -> ((PSymbol) e).getName()).collect(Collectors.toSet());
        Set<String> expectedNames =
                Arrays.asList("g", "u", "z", "f", "w", "x", "b", "k").stream()
                .collect(Collectors.toSet());
        Assert.assertEquals(expectedNames.size(), incomingNames.size());
        Assert.assertEquals(true, incomingNames.containsAll(expectedNames));
    }

    @Test public void testGetQuantifiedVariables() {
        TypeGraph g = new TypeGraph();
        PExp result =
                parseMathAssertionExp(
                        g,
                        "Forall x, y, z : Z, Exists u, v : N," +
                                "Forall f, h : Z * Z -> B, "
                                + "g(@u) + (h(@z, @w, f(@u)))");
        Set<String> quantifiedNames = result.getQuantifiedVariables().stream()
                .map(e -> ((PSymbol)e).getName()).collect(Collectors.toSet());
        Set<String> expectedNames = Arrays.asList("u", "z", "f", "h").stream()
                .collect(Collectors.toSet());
        Assert.assertEquals(4, quantifiedNames.size());
        Assert.assertEquals(true, quantifiedNames.containsAll(expectedNames));
    }

    @Test public void testGetFunctionApplications() {}

    @Test public void testGetSymbolNames() {
       TypeGraph g = new TypeGraph();
        PExp result = parseMathAssertionExp(g, "x + y");
        Set<String> expectedNames = Arrays.asList("x", "+", "y").stream()
                .collect(Collectors.toSet());
        Set<String> foundNames = result.getSymbolNames();
        Assert.assertEquals(expectedNames.size(), foundNames.size());
        Assert.assertEquals(true, foundNames.containsAll(expectedNames));

        result = parseMathAssertionExp(g, "x + y"); //you actually have to do this again or else we'll retrieve a cached answer
        foundNames = result.getSymbolNames(true, false); //now ignoring function applications..
        expectedNames = Arrays.asList("x", "y").stream()
                .collect(Collectors.toSet());
        Assert.assertEquals(expectedNames.size(), foundNames.size());
        Assert.assertEquals(true, foundNames.containsAll(expectedNames));

        result = parseMathAssertionExp(g, "v + y - (Reverse(s)) + x(z, v)"); //you actually have to do this again or else we'll retrieve a cached answer
        foundNames = result.getSymbolNames(true, false); //now ignoring function applications..
        expectedNames = Arrays.asList("y", "s", "z", "v").stream()
                .collect(Collectors.toSet());
        Assert.assertEquals(expectedNames.size(), foundNames.size());
        Assert.assertEquals(true, foundNames.containsAll(expectedNames));

        result = parseMathAssertionExp(g, "v + y - (Reverse(s)) + x(z, v)");
        foundNames = result.getSymbolNames();
        expectedNames = Arrays.asList("v", "y", "Reverse", "s", "x", "z", "+", "-").stream()
                .collect(Collectors.toSet());
        Assert.assertEquals(expectedNames.size(), foundNames.size());
        Assert.assertEquals(true, foundNames.containsAll(expectedNames));

        result = parseMathAssertionExp(g, "5 + 1 - (Reverse(4)) + x(z, v)");
        foundNames = result.getSymbolNames(false, true);
        expectedNames = Arrays.asList("v", "Reverse", "x", "z", "+", "-").stream()
                .collect(Collectors.toSet());
        Assert.assertEquals(expectedNames.size(), foundNames.size());
        Assert.assertEquals(true, foundNames.containsAll(expectedNames));

        result = parseMathAssertionExp(g, "5 + 1 - (Reverse(4)) + x(z, v)");
        foundNames = result.getSymbolNames(false, false);
        expectedNames =
                Arrays.asList("5", "1", "4", "v", "Reverse", "x", "z", "+", "-")
                        .stream().collect(Collectors.toSet());
        Assert.assertEquals(expectedNames.size(), foundNames.size());
        Assert.assertEquals(true, foundNames.containsAll(expectedNames));

        //This one's pretty good because it has PLambda's and PAlternatives
        //inside. So it tests the implementation of getSymbolNames() in those
        //classes..
        result = parseMathAssertionExp(g, "((((SCD(k, conc.P.Trmnl_Loc)) <= Max_Length) and " +
                "(conc.P.Curr_Loc is_in (Inward_Loc(conc.P.Trmnl_Loc)))) and " +
                "({{(P.Labl((SCD(k, conc.P.Trmnl_Loc)))) if (((SCD(k, conc.P.Trmnl_Loc)) + 1) <= P.Length);" +
                "   T.Base_Point otherwise;}} = T.Base_Point))");
        foundNames = result.getSymbolNames(true, true);
        expectedNames =
                Arrays.asList("k", "conc.P.Trmnl_Loc", "conc.P.Curr_Loc",
                        "P.Length", "T.Base_Point", "Max_Length")
                        .stream().collect(Collectors.toSet());
        Assert.assertEquals(expectedNames.size(), foundNames.size());
        Assert.assertEquals(true, foundNames.containsAll(expectedNames));
    }

    @Test public void testSubstitute() {
        TypeGraph g = new TypeGraph();
    }

    //Todo: These should be redone and retested after thinking more about
    //parenthesization and consulting sami & murali w/ several test cases.
    @Test public void testPartition() {
        TypeGraph g = new TypeGraph();

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

        e = parseMathAssertionExp(g, "(P implies Q)");
        partitions = e.splitIntoSequents();
        Assert.assertEquals(1, partitions.size());
        Assert.assertEquals("(P implies Q)", partitions.get(0).toString());

        e = parseMathAssertionExp(g, "P");
        partitions = e.splitIntoSequents();
        Assert.assertEquals(1, partitions.size());
        Assert.assertEquals("(true implies P)", partitions.get(0).toString());

        e = parseMathAssertionExp(g, "(A and (P implies Q))");
        partitions = e.splitIntoSequents();
        Assert.assertEquals(2, partitions.size());
        Assert.assertEquals("(true implies A)", partitions.get(0).toString());
        Assert.assertEquals("(P implies Q)", partitions.get(1).toString());

        e = parseMathAssertionExp(g, "(A implies (B implies (C implies (D and (E and (F and G))))))");
        partitions = e.splitIntoSequents();
        Assert.assertEquals(4, partitions.size());
        Assert.assertEquals("(((A and B) and C) implies D)", partitions.get(0).toString());
        Assert.assertEquals("(((A and B) and C) implies E)", partitions.get(1).toString());
        Assert.assertEquals("(((A and B) and C) implies F)", partitions.get(2).toString());
        Assert.assertEquals("(((A and B) and C) implies G)", partitions.get(3).toString());

        e = parseMathAssertionExp(g, "((A implies (B implies (C implies D))) and (E implies (F implies (G implies (H implies I)))))");
        //e = parseMathAssertionExp(g, "(((1 <= Max_Depth) implies  ((|S| <= Max_Depth) implies  (Temp = Empty_String implies      S = (Reverse(Temp) o S)))) and  ((1 <= Max_Depth) implies  ((|S| <= Max_Depth) implies  (S = (Reverse(Temp') o S_p) implies  (not((1 <= |S_p|)) implies      Temp_p = Reverse(S))))))");
        partitions = e.splitIntoSequents();
        Assert.assertEquals(2, partitions.size());
        Assert.assertEquals("(((A and B) and C) implies D)", partitions.get(0).toString());
        Assert.assertEquals("((((E and F) and G) and H) implies I)", partitions.get(1).toString());

        //e = parseMathAssertionExp(g, "(((0 <= 0) and  ((1 <= max_int) implies  ((min_int <= 0) implies  ((Max_Depth <= max_int) implies  ((min_int <= Max_Depth) implies  ((1 <= Max_Depth) implies  (0 <= Max_Depth))))))) and  (Array_Is_Initial_in_Range(S.Contents, Lower_Bound, Upper_Bound) implies      Reverse(Iterated_Concatenation(1, 0, lambda ( i : Z ).(<S.Contents(i)>))) = Empty_String))");
        //e = parseMathAssertionExp(g, "(((1 <= Max_Depth) implies  ((|S| <= Max_Depth) implies  (Temp = Empty_String implies      S = (Reverse(Temp) o S)))) and  ((1 <= Max_Depth) implies  ((|S| <= Max_Depth) implies  (S = (Reverse(Temp') o S_p) implies  (not((1 <= |S_p|)) implies      Temp_p = Reverse(S))))))");
        //partitions = e.splitIntoSequents();
        //Assert.assertEquals(2, partitions.size());
        //Assert.assertEquals("(((A and B) and C) implies D)", partitions.get(0).toString());
        //Assert.assertEquals("((((E and F) and G) and H) implies I)", partitions.get(1).toString());
    }*/

    protected static ParseTree getTree(String input) {
        try {
            ANTLRInputStream in = new ANTLRInputStream(new StringReader(input));
            ResolveLexer lexer = new ResolveLexer(in);
            TokenStream tokens = new CommonTokenStream(lexer);
            ResolveParser parser = new ResolveParser(tokens);

            //Todo: For some reason this never seems to be getting tripped atm,
            //even in the presence of errors.
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
     *
     * <p>
     * Building even moderately sized {@link PExp}s is a pain; building one
     * with real type information is an even bigger pain. Thus, for test methods
     * where this function is used, know that we don't care about types so much
     * as we do about correct expression structure and quantifier
     * distribution.</p>
     *
     * <p>
     * In other words, if you want to test something math type related, just
     * construct smaller exprs manually using {@link PSymbol.PSymbolBuilder},
     * otherwise parse the actual larger expr using this method.</p>
     *
     * @param input The input to parse.
     * @return The dummy-typed {@link PExp} representation of {@code input}.
     */
    @NotNull public static PExp parseMathAssertionExp(@NotNull TypeGraph g,
                                                      @NotNull String input) {
        ParseTree t = getTree(input);
        AnnotatedTree dummy = new AnnotatedTree(t, "test", null, false);
        PExpBuildingListener<PExp> l =
                new PExpBuildingListener<>(dummy, g.INVALID); //dummyType
        ParseTreeWalker.DEFAULT.walk(l, t);
        return l.getBuiltPExp(t);
    }
}
