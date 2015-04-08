package org.resolvelite.semantics.symbol;

import org.resolvelite.semantics.Scope;
import org.resolvelite.semantics.Type;

import java.util.Map;

// Todo: Add a getRootModuleID() method (right now only SymbolsWithScope get
// this
public interface Symbol {

    public String getName();

    public Scope getScope();

    public void setScope(Scope scope);

    public String getRootModuleID();

    //force implementors to write equals and hashcode
    //so symbols can be properly used in collections such
    //as sets, etc.
    int hashCode();

    boolean equals(Object o);

    public Symbol substituteGenerics(
            Map<GenericSymbol, Type> genericSubstitutions,
            Scope scopeWithSubstitutions);
}
