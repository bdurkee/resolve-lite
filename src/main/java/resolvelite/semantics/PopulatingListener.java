package resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import resolvelite.compiler.ResolveCompiler;
import resolvelite.parsing.ResolveBaseListener;
import resolvelite.parsing.ResolveParser;

public class PopulatingListener extends ResolveBaseListener {

    Scope currentScope; // define symbols in this scope
    ResolveCompiler compiler;
    SymbolTable symtab;

    public PopulatingListener(@NotNull ResolveCompiler rc) {
        this.compiler = rc;
        this.symtab = rc.symbolTable;
    }

    @Override
    public void enterPrecisModule(
            @NotNull ResolveParser.PrecisModuleContext ctx) {
        currentScope = establishModuleScope(ctx.name.getText(), ctx);
    }

    @Override
    public void enterConceptModule(
            @NotNull ResolveParser.ConceptModuleContext ctx) {
        currentScope = establishModuleScope(ctx.name.getText(), ctx);
    }

    @Override
    public void enterMathDefinitionDecl(
            @NotNull ResolveParser.MathDefinitionDeclContext ctx) {
        String name = ctx.name.getText();

        // push new scope by making new one that points to enclosing scope
        MathSymbol mathSymFxn =
                new MathSymbol(symtab.getTypeGraph(), name, ctx);
        mathSymFxn.setEnclosingScope(currentScope);

        currentScope.define(mathSymFxn); // Define function in current scope
        symtab.scopes.put(ctx, mathSymFxn); // Push: set function's parent to current
        currentScope = mathSymFxn; // Current scope is now function scope
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
