package org.resolvelite.semantics.symbol;

import org.resolvelite.semantics.Scope;
import org.resolvelite.semantics.Type;

import java.util.Map;

public class GenericSymbol extends BaseSymbol implements Type {

    public GenericSymbol(String name, String rootModuleID) {
        super(name, rootModuleID);
    }

    @Override public Symbol substituteGenerics(
            Map<GenericSymbol, Type> genericSubstitutions,
            Scope scopeWithSubstitutions) {
        return new GenericSymbol(genericSubstitutions.get(this).getName(),
                rootModuleID);
    }
}
