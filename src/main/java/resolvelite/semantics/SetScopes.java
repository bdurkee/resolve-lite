package resolvelite.semantics;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import resolvelite.parsing.ResolveBaseListener;
import resolvelite.parsing.ResolveParser;

class SetScopes extends ResolveBaseListener {

    private final ParseTreeProperty<Scope> establishedScopes;
    protected Scope currentScope;

    SetScopes(SymbolTable symtab, @NotNull DefSymbolsAndScopes scopeRepo) {
        this.establishedScopes = scopeRepo.scopes;
    }

    @Override
    public void enterModule(@NotNull ResolveParser.ModuleContext ctx) {
        currentScope = establishedScopes.get(ctx);
    }

    @Override
    public void enterMathDefinitionDecl(
            @NotNull ResolveParser.MathDefinitionDeclContext ctx) {
        currentScope = establishedScopes.get(ctx);
    }

}
