package resolvelite.semantics;

import org.antlr.v4.runtime.misc.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class BaseScope implements Scope {

    private Scope enclosingScope; // null if we're the global (outermost) scope

    /**
     * Use a linked hashmap to preserve entry order.
     */
    private final Map<String, Symbol> symbols =
            new LinkedHashMap<String, Symbol>();

    public BaseScope(@Nullable Scope enclosingScope) {
        this.enclosingScope = enclosingScope;
    }

    @Override
    public void define(Symbol sym) {
        symbols.put(sym.name, sym);
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
        return getScopeName()+":"+symbols.keySet().toString();
    }

}
