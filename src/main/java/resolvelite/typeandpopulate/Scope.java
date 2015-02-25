package resolvelite.typeandpopulate;

import resolvelite.typeandpopulate.entry.SymbolTableEntry;

public interface Scope {

    public String getScopeName();

    /** Where to look next for symbols */
    public Scope getEnclosingScope();

    /** Look up name in this scope or in enclosing scope if not here */
    public SymbolTableEntry resolve(String name);
}
