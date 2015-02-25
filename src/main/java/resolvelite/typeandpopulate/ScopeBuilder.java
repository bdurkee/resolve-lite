package resolvelite.typeandpopulate;

import resolvelite.typeandpopulate.entry.SymbolTableEntry;

import java.util.HashMap;
import java.util.Map;

public class ScopeBuilder implements Scope {

    private final Map<String, SymbolTableEntry> symbols =
            new HashMap<String, SymbolTableEntry>();

    public ScopeBuilder() {
        //define(new BuiltInTypeSymbol("B"));
    }

    protected void initMathTypeSystem() {

    }

    @Override public String getScopeName() { return "global"; }

    @Override public Scope getEnclosingScope() {
        return null;
    }

    //addbinding methods go here.

    @Override public SymbolTableEntry resolve(String name) {
        return symbols.get(name);
    }

    @Override public String toString() { return getScopeName()+":"+symbols; }
}
