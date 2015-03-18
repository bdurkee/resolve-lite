package resolvelite.semantics;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import resolvelite.parsing.ResolveBaseListener;
import resolvelite.parsing.ResolveParser;

public class DefSymbolsAndScopes extends ResolveBaseListener {

    public final ParseTreeProperty<Scope> scopes =
            new ParseTreeProperty<Scope>();
    Scope globalModuleScope;
    Scope currentScope; // define symbols in this scope

    @Override
    public void enterPrecisModule(
            @NotNull ResolveParser.PrecisModuleContext ctx) {

    }
    @Override
    public void enterMathDefinitionDecl(
            @NotNull ResolveParser.MathDefinitionDeclContext ctx) {
      //  String name = ctx.name.getText();
      //  int typeTokenType = ctx.type().start.getType();
      //  Symbol.Type type = CheckSymbols.getType(typeTokenType);

        // push new scope by making new one that points to enclosing scope
      //  FunctionSymbol function = new FunctionSymbol(name, type, currentScope);
      //  currentScope.define(function); // Define function in current scope
      //  saveScope(ctx, function); // Push: set function's parent to current
     //   currentScope = function; // Current scope is now function scope
    }

}
