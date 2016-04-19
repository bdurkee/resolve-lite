package edu.clemson.resolve;

import edu.clemson.resolve.compiler.AnnotatedModule;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

public class TestMathSymbolTable extends BaseTest {

    //@Test(expected=NoSuchModuleException.class)
    public void testFreshMathSymbolTable1() {
      //  MathSymbolTable b = new MathSymbolTable();
      //  b.getModuleScope("NonExistent");
    }

    /*@Test(expected=IllegalArgumentException.class)
    public void testFreshMathSymbolTable2()
            throws IllegalArgumentException {
        MathSymbolTable b = new MathSymbolTable();
        ParserRuleContext someFakeContext =
                new ResolveParser.OperationDeclContext(null, 0);
        b.getScope(someFakeContext);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testFreshMathSymbolTable3() {
        MathSymbolTable b = new MathSymbolTable();
        b.startModuleScope(null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testFreshMathSymbolTable4() {
        MathSymbolTable b = new MathSymbolTable();
        b.startScope(null);
    }

    @Test public void testStartModuleScope1() {
        MathSymbolTable b = new MathSymbolTable();

        AnnotatedModule m = buildDummyModule("Precis Foo;\n end Foo;");
        b.startModuleScope(m);
        b.endScope();

        ModuleScopeBuilder s = b.getModuleScope("Foo");
        Assert.assertTrue(s.getDefiningTree() == m.getRoot());
    }

    @Test(expected=IllegalStateException.class)
    public void testScopeGetInnermostBinding1()
            throws IllegalStateException {

        MathSymbolTable b = new MathSymbolTable();

        AnnotatedModule m = buildDummyModule("Precis Foo;\n end Foo;");
        b.startModuleScope(m);
        b.endScope();

        ModuleScopeBuilder s = b.getModuleScope("Foo");
        b.getInnermostActiveScope();
    }

    @Test public void testScopeGetInnermostBinding2()
            throws NoSuchSymbolException, DuplicateSymbolException,
            NoSuchModuleException {

       MathSymbolTable b = new MathSymbolTable();

        AnnotatedModule m = buildDummyModule("Precis Foo;\n end Foo;");
        ScopeBuilder s = b.startModuleScope(m);
        s.addBinding("E", myConceptualElement1, myType1);
        b.endScope();

        MathSymbolTable t = b.seal();

        ModuleScope ms = t.getModuleScope(new ModuleIdentifier("x"));
        MathSymbolTableEntry e = ms.getInnermostBinding("E");

        assertEquals(e.getDefiningElement(), myConceptualElement1);
        assertEquals(e.getNameToken(), "E");
        assertEquals(e.getType(), myType1);
    }

    private AnnotatedModule buildDummyModule(String moduleString) {
        try {
            ANTLRInputStream in = new ANTLRInputStream(
                    new StringReader(moduleString));
            ResolveLexer lexer = new ResolveLexer(in);
            TokenStream tokens = new CommonTokenStream(lexer);
            ResolveParser parser = new ResolveParser(tokens);

            if ( parser.getNumberOfSyntaxErrors() > 0 ) {
                throw new IllegalArgumentException("moduleString contains " +
                        "syntax errors");
            }
            return new AnnotatedModule(parser.module(), "Foo");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }*/
}
