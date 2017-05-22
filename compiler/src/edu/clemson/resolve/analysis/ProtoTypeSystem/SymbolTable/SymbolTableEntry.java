package edu.clemson.resolve.analysis.ProtoTypeSystem.SymbolTable;

import edu.clemson.resolve.analysis.ProtoTypeSystem.Types.MTEntity;

public class SymbolTableEntry {
    private String mySymbol;
    private MTEntity myType, myTypeValue;

    public SymbolTableEntry(String symbol, MTEntity type) {
        mySymbol = symbol;
        myType = type;
    }

    public SymbolTableEntry(String symbol, MTEntity type, MTEntity typeValue) {
        mySymbol = symbol;
        myType = type;
        myTypeValue = typeValue;
    }

    public String getSymbol() {
        return mySymbol;
    }

    public MTEntity getType() {
        return myType;
    }

    public MTEntity getTypeValue() {
        return myTypeValue;
    }

    @Override
    public String toString() {
        return String.format("[Symbol %s of type %s]", mySymbol, myType.toString());
    }
}
