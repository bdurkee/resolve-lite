package resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import resolvelite.compiler.ResolveCompiler;
import resolvelite.parsing.ResolveBaseListener;
import resolvelite.parsing.ResolveParser;

class SetScopes extends ResolveBaseListener {

    @NotNull protected ResolveCompiler compiler;
    @NotNull protected SymbolTable symtab;
    @NotNull protected Scope currentScope;

    SetScopes(@NotNull ResolveCompiler rc, @NotNull SymbolTable symtab) {
        this.compiler = rc;
        this.symtab = symtab;
    }

    @Override public void enterFacilityModule(
            @NotNull ResolveParser.FacilityModuleContext ctx) {
        currentScope = symtab.moduleScopes.get(ctx.name.getText());
    }

    @Override public void enterConceptModule(
            @NotNull ResolveParser.ConceptModuleContext ctx) {
        currentScope = symtab.moduleScopes.get(ctx.name.getText());
    }

    @Override public void enterPrecisModule(
            @NotNull ResolveParser.PrecisModuleContext ctx) {
        currentScope = symtab.moduleScopes.get(ctx.name.getText());
    }

    /**
     * Sets current scope ptr on pre-traversal; {@link ComputeTypes} resolves
     * references to declared syms on the post traversal.
     * 
     * @param ctx
     */
    @Override public void enterEveryRule(@NotNull ParserRuleContext ctx) {
        if ( symtab.scopes.get(ctx) != null ) {
            currentScope = symtab.scopes.get(ctx);
        }
    }
}
