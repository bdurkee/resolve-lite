package edu.clemson.resolve.analysis.ProtoTypeSystem.SymbolTable;

import edu.clemson.resolve.semantics.DuplicateSymbolException;
import edu.clemson.resolve.semantics.NoSuchSymbolException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Scope {
    private Scope myParentScope;
    private Set<Scope> myChildScopes;
    private Map<String, SymbolTableEntry> myEntries;

    public Scope(Scope parentScope) {
        myParentScope = parentScope;
        myChildScopes = new HashSet<>();
        myEntries = new HashMap<>();
    }

    public Scope getParentScope() {
        return myParentScope;
    }

    public Scope createChildScope() {
        Scope innerScope = new Scope(this);
        myChildScopes.add(innerScope);
        return innerScope;
    }

    public void addEntry(SymbolTableEntry entry) throws DuplicateSymbolException {
        if (myEntries.containsKey(entry.getSymbol())) {
            throw new DuplicateSymbolException();
        }
        myEntries.put(entry.getSymbol(), entry);
    }

    public SymbolTableEntry getEntry(String symbol) throws NoSuchSymbolException {
        if (myEntries.containsKey(symbol)) {
            return myEntries.get(symbol);
        }

        if (myParentScope == null) {
            throw new NoSuchSymbolException();
        }

        return myParentScope.getEntry(symbol);
    }
}
