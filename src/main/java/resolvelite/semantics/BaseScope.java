package resolvelite.semantics;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTree;
import resolvelite.semantics.symbol.Symbol;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class BaseScope implements Scope {

    private Scope enclosingScope; //null if we're the global (outermost) scope

    private final Map<String, Symbol> symbols =
            new LinkedHashMap<String, Symbol>();

    public BaseScope(@NotNull ScopeRepository source,
                     @Nullable ParseTree definingTree, @Nullable Scope parent,
                     @NotNull ModuleIdentifier enclosingModule) {
        this.enclosingScope = enclosingScope;
    }

    @Override
    public void define(Symbol sym) {
        symbols.put(sym.getName(), sym);
        sym.scope = this; // track the scope in each symbol
    }

    @Override
    public Symbol resolve(String name) {
        Symbol s = symbols.get(name);
        if ( s != null ) {
            return s;
        }
        // if not here, check any enclosing scope
        if ( enclosingScope != null ) {
            return enclosingScope.resolve(name);
        }
        return null; // not found
    }

    @Override
    public Scope getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public String toString() {
        return getScopeName() + ":" + symbols.keySet().toString();
    }

}
