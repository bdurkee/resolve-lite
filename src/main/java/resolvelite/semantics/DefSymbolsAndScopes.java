package resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import resolvelite.compiler.ErrorKind;
import resolvelite.compiler.ResolveCompiler;
import resolvelite.parsing.ResolveBaseListener;
import resolvelite.parsing.ResolveParser;

public class DefSymbolsAndScopes extends ResolveBaseListener {

    Scope currentScope; // define symbols in this scope
    ResolveCompiler compiler;
    SymbolTable symtab;

    public DefSymbolsAndScopes(@NotNull ResolveCompiler rc) {
        this.compiler = rc;
        this.symtab = rc.symbolTable;
    }

    @Override
    public void
            enterPrecisModule(@NotNull ResolveParser.PrecisModuleContext ctx) {
        currentScope = establishModuleScope(ctx.name.getText(), ctx);
    }

    @Override
    public void enterConceptModule(
            @NotNull ResolveParser.ConceptModuleContext ctx) {
        currentScope = establishModuleScope(ctx.name.getText(), ctx);
    }

    @Override
    public void enterTypeModelDecl(
            @NotNull ResolveParser.TypeModelDeclContext ctx) {
        String name = ctx.name.getText();
    }

    @Override
    public void enterMathDefinitionDecl(
            @NotNull ResolveParser.MathDefinitionDeclContext ctx) {
        String name = ctx.name.getText();
        ComputeTypes resolver = new ComputeTypes(symtab, currentScope);
        ParseTreeWalker.DEFAULT.walk(resolver, ctx.mathTypeExp());
        MathType declaredType = resolver.mathTypeValues.get(ctx.mathTypeExp());
        MathType typeValue = null;
        if ( ctx.mathAssertionExp() != null ) { //if the def. has an rhs.
            typeValue = resolver.mathTypeValues.get(ctx.mathAssertionExp());
        }
        // push new scope by making new one that points to enclosing scope
        try {
            MathSymbol mathSymFxn =
                    new MathSymbol(symtab.getTypeGraph(), name, declaredType,
                            typeValue, ctx);
            mathSymFxn.setEnclosingScope(currentScope);

            currentScope.define(mathSymFxn); // Define def in current scope
            symtab.scopes.put(ctx, mathSymFxn); // Push: set def's parent to current
            currentScope = mathSymFxn; // Current scope is now def scope
        }
        catch (IllegalArgumentException iae) {
            symtab.getCompiler().errorManager.semanticError(
                    ErrorKind.DUP_SYMBOL, ctx.name, ctx.name.getText());
        }
    }

    @Override
    public void exitMathDefinitionDecl(
            @NotNull ResolveParser.MathDefinitionDeclContext ctx) {
        currentScope = currentScope.getEnclosingScope(); // pop scope
    }

    private ModuleScope establishModuleScope(@NotNull String moduleName,
            @NotNull ParserRuleContext ctx) {
        ModuleScope module = new ModuleScope(PredefinedScope.INSTANCE);
        symtab.moduleScopes.put(moduleName, module);
        return module;
    }
}
