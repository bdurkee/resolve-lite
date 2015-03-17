package resolvelite.semantics;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import resolvelite.parsing.ResolveBaseListener;
import resolvelite.parsing.ResolveParser;

public class DefSymbolsAndScopes extends ResolveBaseListener {

    public final ParseTreeProperty<Scope> scopes =
            new ParseTreeProperty<Scope>();
    ModuleScope globals;
    Scope currentScope; // define symbols in this scope

    public DefSymbolsAndScopes(ModuleScope globals) {
        globals = new ModuleScope(null);
        currentScope = globals;
    }

    @Override
    public void enterModule(@NotNull ResolveParser.ModuleContext ctx) {

    }

}
