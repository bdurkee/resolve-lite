package resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import resolvelite.parsing.ResolveBaseListener;
import resolvelite.parsing.ResolveParser;

/**
 * Descends the nodes of a parse tree, setting a scope pointer for each subtree
 * reached that defines a scope.
 */
class SetScopes extends ResolveBaseListener {

    private final ParseTreeProperty<Scope> establishedScopes;
    protected Scope currentScope;
    protected SymbolTable symbolTable;

    SetScopes(SymbolTable symtab, @NotNull DefSymbolsAndScopes scopeRepo) {
        this.symbolTable = symtab;
        this.establishedScopes = scopeRepo.scopes;
    }

    /**
     * {@inheritDoc}
     *
     * Sets current scope pointer to the appropriate pre-established scope
     * for the section of the tree getting traversed.
     */
    @Override public void enterEveryRule(ParserRuleContext ctx) {
        if (establishedScopes.get(ctx) != null) {
            currentScope = establishedScopes.get(ctx);
            symbolTable.getCompiler().info("scope ptr -> " + currentScope);
        }
    }
}
