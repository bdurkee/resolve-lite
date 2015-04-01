package org.resolvelite.semantics.symbol;

import org.resolvelite.semantics.Scope;

public interface Symbol {

    public String getName();

    public Scope getScope();

    public void setScope(Scope scope);

    //force implementors to write equals and hashcode
    //so symbols can be properly used in collections such
    //as sets, etc.
    int hashCode();

    boolean equals(Object o);

    //todo: this should be changed to ProgTypeSymbol in the future once we kill
    //the hardcoded defs of Integer and Boolean.
}
