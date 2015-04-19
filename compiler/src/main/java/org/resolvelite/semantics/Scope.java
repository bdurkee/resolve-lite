package org.resolvelite.semantics;

import org.resolvelite.semantics.symbol.Symbol;

import java.util.List;

public interface Scope {

    public <T extends Symbol> List<T> getSymbolsOfType(Class<T> type);

    public Symbol define(Symbol s) throws DuplicateSymbolException;
}
