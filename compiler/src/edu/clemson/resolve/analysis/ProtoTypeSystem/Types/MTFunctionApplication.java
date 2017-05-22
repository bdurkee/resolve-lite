package edu.clemson.resolve.analysis.ProtoTypeSystem.Types;

import edu.clemson.resolve.analysis.ProtoTypeSystem.SymbolTable.SymbolTableEntry;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class MTFunctionApplication extends MTEntity {
    public static final TypeFactory POWERCLASS_FACTORY = (String canonicalName, MTEntity knownSupertype) -> new MTClass(canonicalName, knownSupertype);
    public static final TypeFactory POWERSET_FACTORY = (String canonicalName, MTEntity knownSupertype) -> new MTSet(canonicalName, knownSupertype);

    private MTFunction myFunction;
    private List<SymbolTableEntry> myArguments;

    public MTFunctionApplication(MTFunction function, List<SymbolTableEntry> arguments) {
        myFunction = function;
        myArguments = new LinkedList<>(arguments);
    }

    @Override
    public boolean isKnownType() {
        return myFunction.getRangeType().isKnownType();
    }

    @Override
    public MTEntity createElementType(String canonicalName) {
        return myTypeFactory.createElementType(canonicalName, myArguments.get(0).getTypeValue());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(myFunction.getCanonicalName());
        sb.append("(");
        Iterator<SymbolTableEntry> iter = myArguments.iterator();
        while (iter.hasNext()) {
            sb.append(iter.next().getSymbol());
            if (iter.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }
}
