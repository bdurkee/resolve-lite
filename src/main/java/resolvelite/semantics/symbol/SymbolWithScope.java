package resolvelite.semantics.symbol;

import resolvelite.semantics.BaseScope;
import resolvelite.semantics.Scope;
import resolvelite.semantics.SymbolTable;
import resolvelite.semantics.UnexpectedSymbolException;

public abstract class SymbolWithScope extends BaseScope
        implements
            Scope,
            Symbol {

    protected final String name; // All symbols at least have a name
    protected int index; // insertion order from 0; compilers often need this

    public SymbolWithScope(String name, SymbolTable scopeRepo,
            String rootModuleID) {
        super(scopeRepo, rootModuleID);
        this.name = name;
    }

    /*@Override public ProgTypeDefinitionSymbol toProgTypeDefSym()
            throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException();
    }*/

    @Override public String getName() {
        return name;
    }

    @Override public Scope getScope() {
        return enclosingScope;
    }

    @Override public void setScope(Scope scope) {
        setEnclosingScope(scope);
    }

    public Scope getParentScope() {
        return getEnclosingScope();
    }

    public Scope getEnclosingScope() {
        return enclosingScope;
    }

    public String getScopeDescription() {
        return name;
    }

    @Override public boolean equals(Object obj) {
        if ( !(obj instanceof Symbol) ) return false;
        if ( obj == this ) return true;
        return name.equals(((Symbol) obj).getName());
    }

    @Override public int hashCode() {
        return name.hashCode();
    }

}
