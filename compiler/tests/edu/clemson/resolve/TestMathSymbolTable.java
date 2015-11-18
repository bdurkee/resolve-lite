package edu.clemson.resolve;

import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.parser.ResolveParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Assert;
import org.junit.Test;
import org.rsrg.semantics.*;

public class TestMathSymbolTable extends BaseTest {

    @Test(expected=NoSuchModuleException.class)
    public void testFreshMathSymbolTable()
            throws NoSuchModuleException {
        MathSymbolTable b = new MathSymbolTable();
        b.getModuleScope("NonExistent");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testFreshMathSymbolTable2()
            throws IllegalArgumentException {
        MathSymbolTable b = new MathSymbolTable();
        ParserRuleContext someFakeContext =
                new ResolveParser.OperationDeclContext(null, 0);
        b.getScope(someFakeContext);
    }

    @Test public void testStartModuleScope1() {
        MathSymbolTable b = new MathSymbolTable();

        ParseTree dummyModuleCtx =
                parseModuleFromString("Precis Foo;\n end Foo;");
        AnnotatedTree m = new AnnotatedTree(dummyModuleCtx, "Foo");
        b.startModuleScope(m);
        b.endScope();

        ModuleScopeBuilder s = b.getModuleScope("Foo");
        Assert.assertTrue(s.getDefiningTree() == m.getRoot());
    }

    @Test(expected=IllegalStateException.class)
    public void testScopeGetInnermostBinding1()
            throws IllegalStateException {

        MathSymbolTable b = new MathSymbolTable();

        ParseTree dummyModuleCtx =
                parseModuleFromString("Precis Foo;\n end Foo;");
        AnnotatedTree m = new AnnotatedTree(dummyModuleCtx, "Foo");

        b.startModuleScope(m);
        b.endScope();

        ModuleScopeBuilder s = b.getModuleScope("Foo");
        b.getInnermostActiveScope();
    }

    @Test public void testScopeGetInnermostBinding2()
            throws NoSuchSymbolException, DuplicateSymbolException,
            NoSuchModuleException {

        MathSymbolTable b = new MathSymbolTable();

        MathModuleDec m = new MathModuleDec(myPosSymbol1, null, null, null);
        ScopeBuilder s = b.startModuleScope(m);
        s.addBinding("E", myConceptualElement1, myType1);
        b.endScope();

        MathSymbolTable t = b.seal();

        ModuleScope ms = t.getModuleScope(new ModuleIdentifier("x"));
        MathSymbolTableEntry e = ms.getInnermostBinding("E");

        assertEquals(e.getDefiningElement(), myConceptualElement1);
        assertEquals(e.getName(), "E");
        assertEquals(e.getType(), myType1);
    }

}
