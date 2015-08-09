package edu.clemson.resolve;

import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.parser.Resolve;
import edu.clemson.resolve.parser.ResolveLexer;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpBuildingListener;
import edu.clemson.resolve.proving.absyn.PSymbol;
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

    @Test public void testQuantifierDistribution() {
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
                parseMathAssertionExp(
                        g,
                        "Forall x, y, z : Z, Exists u, v, w : N,"
                                + "g(@u) + (h(@z, @w, f(@u)))");
        Set<String> incomingNames = result.getIncomingVariables().stream()
                .map(e -> ((PSymbol) e).getName()).collect(Collectors.toSet());
        Assert.assertEquals(3, incomingNames.size());
        Set<String> expectedNames = Arrays.asList("u", "z", "w").stream()
                .collect(Collectors.toSet());
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

    @Test public void testGetSymbolNames() {}

    @Test public void testSubstitute() {}

    @Test public void testPartition() {
        TypeGraph g = new TypeGraph();
   /*     PExp e = parseMathAssertionExp(g, "x implies y");
        Assert.assertEquals(1, e.partition().size());
        Assert.assertEquals("(x implies y)", e.partition().get(0).toString());

        e = parseMathAssertionExp(g, "x and y implies z");
        Assert.assertEquals(1, e.partition().size());
        Assert.assertEquals("((x and y) implies z)", e.partition().get(0).toString());

        e = parseMathAssertionExp(g, "x and y and z implies f");
        Assert.assertEquals(1, e.partition().size());
        Assert.assertEquals("(((x and y) and z) implies f)", e.partition().get(0).toString());
*/
        //since ' (or ?) are are (rightly) not recognized by the parser or lexer,
        //I'm using *_pp .. to indicate how many 'p'rimes mark a variable
        PExp e = parseMathAssertionExp(g, "1 <= Max_Depth implies |S| <= Max_Depth implies Temp = Empty_String implies S = Reverse(Temp) o S and " +
                "1 <= Max_Depth implies |S| <= Max_Depth implies S = Reverse(Temp_p) o S_pp implies 1 <= |S_pp| implies 1 <= |S_pp|");//and " +
            //    "1 <= Max_Depth implies |S| <= Max_Depth implies S = Reverse(Temp_p) o S_pp implies 1 <= |S_pp| implies 1 + |Temp_p| <= Max_Depth and " +
            //    "1 <= Max_Depth implies |S| <= Max_Depth implies S = Reverse(Temp_p) o S_pp implies 1 <= |S_pp| implies S_pp = <Next_Entry_p> o S_p implies S = Reverse(<Next_Entry_p> o Temp_p) o S_p");
        List<PExp> partitions = e.partition();
      //  Iterator<PExp> partitionIter = partitions.iterator();

      //  Assert.assertEquals(4, partitions.size());
     //   Assert.assertEquals("((((1 <= Max_Depth) and (|S| <= Max_Depth)) and (Temp = Empty_String)) implies (S = ((Reverse(Temp)) o S)))", partitionIter.next().toString());
     //   Assert.assertEquals("(((((1 <= Max_Depth) and (|S| <= Max_Depth)) and (S = ((Reverse(Temp_p)) o S_pp))) and (1 <= |S_pp|)) implies (1 <= |S_pp|))", partitionIter.next().toString());
     //   Assert.assertEquals("(((((1 <= Max_Depth) and (|S| <= Max_Depth)) and (S = ((Reverse(Temp_p)) o S_pp))) and (1 <= |S_pp|)) implies ((1 + |Temp_p|) <= Max_Depth))", partitionIter.next().toString());
     //   Assert.assertEquals("((((((1 <= Max_Depth) and (|S| <= Max_Depth)) and (S = ((Reverse(Temp_p)) o S_pp))) and (1 <= |S_pp|)) and (S_pp = (<Next_Entry_p> o S_p))) implies (S = ((Reverse((<Next_Entry_p> o Temp_p))) o S_p)))", partitionIter.next().toString());
/*
        PExp e = parseMathAssertionExp(g, "b implies y and bv implies a and b and c implies z");
        List<PExp> parts = e.partition();
        Assert.assertEquals(1, parts.size());
        Assert.assertEquals("(((((b and y) and bv) and a) and c) implies z)", e.partition().get(0).toString());*/
    }

    protected static ParseTree getTree(String input) {
        try {
            ANTLRInputStream in = new ANTLRInputStream(new StringReader(input));
            ResolveLexer lexer = new ResolveLexer(in);
            TokenStream tokens = new CommonTokenStream(lexer);
            Resolve parser = new Resolve(tokens);

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
     * <p>
     * Building even moderately sized {@link PExp}s is a pain; building one
     * with real type information is an even bigger pain. Thus, for test methods
     * where this function is used, know that we don't care about types so much
     * as we do about correct expression structure and quantifier
     * distribution.</p>
     * <p>
     * In other words, if you want to test something math type related, just
     * construct smaller exprs manually using {@link PSymbol.PSymbolBuilder},
     * otherwise parse the actual larger expr using this method.</p>
     *
     * @param input The input to parse.
     * @return The dummy-typed {@link PExp} representation of {@code input}.
     */
    public static PExp parseMathAssertionExp(TypeGraph g, String input) {
        ParseTree t = getTree(input);
        AnnotatedTree dummy = new AnnotatedTree(t, "test", null, false);
        PExpBuildingListener<PExp> l =
                new PExpBuildingListener<>(new ParseTreeProperty<>(), dummy,
                        g.INVALID); //dummyType
        ParseTreeWalker.DEFAULT.walk(l, t);
        return l.getBuiltPExp(t);
    }
}
