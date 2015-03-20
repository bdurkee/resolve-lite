package resolvelite.semantics;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import resolvelite.parsing.ResolveBaseListener;
import resolvelite.parsing.ResolveParser;

// TODO: Use a specialized visitor for getting math types for arbitrary
// expression subtrees.
// not sure whether we should do this for PTTypes as well, I don't think we
// really
// use trees for the types in that world. So it might not be necessary.
public class DefSymbolsAndScopes extends ResolveBaseListener {

    ParseTreeProperty<Scope> scopes = new ParseTreeProperty<>();
    ModuleScope moduleScope;
    Scope currentScope; // define symbols in this scope

    @NotNull
    public void enterModule(@NotNull ResolveParser.ModuleContext ctx) {
        //moduleScope = new ModuleScope();
        //currentScope = moduleScope;
    }

}
