package resolvelite.semantics;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import resolvelite.compiler.ResolveCompiler;
import resolvelite.parsing.ResolveBaseListener;
import resolvelite.parsing.ResolveParser;

public class DefSymbolsAndScopes extends ResolveBaseListener {

    ParseTreeProperty<Scope> scopes = new ParseTreeProperty<>();
    Scope currentScope; // define symbols in this scope
    ResolveCompiler compiler;
    SymbolTable symtab;

    public DefSymbolsAndScopes(@NotNull ResolveCompiler rc) {
        this.compiler = rc;
        this.symtab = rc.symbolTable;
    }

    @Override
    public void enterModule(@NotNull ResolveParser.ModuleContext ctx) {
        currentScope = symtab.MODULE;
        scopes.put(ctx, currentScope);
    }

    @Override
    public void enterMathDefinitionDecl(
            @NotNull ResolveParser.MathDefinitionDeclContext ctx) {
        String name = ctx.name.getText();

        // push new scope by making new one that points to enclosing scope
        MathSymbol mathSymFxn = new MathSymbol(symtab.getTypeGraph(), name, ctx);
        mathSymFxn.setEnclosingScope(currentScope);

        currentScope.define(mathSymFxn); // Define function in current scope
        scopes.put(ctx, mathSymFxn); // Push: set function's parent to current
        currentScope = mathSymFxn; // Current scope is now function scope
    }

    @Override
    public void exitMathDefinitionDecl(
            @NotNull ResolveParser.MathDefinitionDeclContext ctx) {
        currentScope = currentScope.getEnclosingScope(); // pop scope
    }
}
