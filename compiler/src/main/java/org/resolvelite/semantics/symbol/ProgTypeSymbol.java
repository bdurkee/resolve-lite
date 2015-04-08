package org.resolvelite.semantics.symbol;

import org.resolvelite.semantics.DuplicateSymbolException;
import org.resolvelite.semantics.Scope;
import org.resolvelite.semantics.SymbolTable;
import org.resolvelite.semantics.Type;

import java.util.Map;

public class ProgTypeSymbol extends SymbolWithScope implements Type {

    public ProgTypeSymbol(String name, SymbolTable scopeRepo,
            String rootModuleID) {
        super(name, scopeRepo, rootModuleID);
    }

    @Override public Symbol substituteGenerics(
            Map<GenericSymbol, Type> genericSubstitutions,
            Scope enclosingSubstitutionScope) {
        return this;
    }
}
