package edu.clemson.resolve.analysis.ProtoTypeSystem.SymbolTable;

import edu.clemson.resolve.analysis.ProtoTypeSystem.Types.MTEntity;
import edu.clemson.resolve.semantics.DuplicateSymbolException;
import edu.clemson.resolve.semantics.NoSuchSymbolException;

public class SymbolTable {

    private Scope myGlobalScope, myOpenScope;
    private SymbolTableEntry myLastEntry;

    public SymbolTable() {
        myGlobalScope = new Scope(null);
        myOpenScope = myGlobalScope;
    }

    public void openScope() {
        myOpenScope = myOpenScope.createChildScope();
    }

    public void closeScope() {
        myOpenScope = myOpenScope.getParentScope();
    }

    public Scope getOpenScope() {
        return myOpenScope;
    }

    public void addEntry(String symbol, MTEntity type) throws DuplicateSymbolException {
        myLastEntry = new SymbolTableEntry(symbol, type, type.createElementType(symbol));
        myOpenScope.addEntry(myLastEntry);
    }

    public void addEntry(String symbol, MTEntity type, MTEntity typeValue) throws DuplicateSymbolException {
        myLastEntry = new SymbolTableEntry(symbol, type, typeValue);
        myOpenScope.addEntry(myLastEntry);
    }

    public SymbolTableEntry getLastEntry() {
        return myLastEntry;
    }

    public SymbolTableEntry getEntry(String symbol) throws NoSuchSymbolException {
        return myOpenScope.getEntry(symbol);
    }
}
