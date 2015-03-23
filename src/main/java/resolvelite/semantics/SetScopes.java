package resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import resolvelite.compiler.ResolveCompiler;
import resolvelite.parsing.ResolveBaseListener;

class SetScopes extends ResolveBaseListener {

    @NotNull protected ResolveCompiler compiler;
    @NotNull protected SymbolTable symtab;
    @NotNull protected Scope currentScope;

    SetScopes(@NotNull ResolveCompiler rc, @NotNull SymbolTable symtab) {
        this.compiler = rc;
        this.symtab = symtab;
    }

    /**
     * Sets current scope ptr on pre-traversal; {@link ComputeTypes} resolves
     * references to declared syms on the post traversal.
     * 
     * @param ctx
     */
    @Override
    public void enterEveryRule(@NotNull ParserRuleContext ctx) {
        if ( symtab.moduleScopes.get(ctx) != null ) {
            currentScope = symtab.moduleScopes.get(ctx);
        }
        else if ( symtab.scopes.get(ctx) != null ) {
            currentScope = symtab.scopes.get(ctx);
        }

    }
}
