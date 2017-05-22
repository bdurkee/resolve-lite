package edu.clemson.resolve.analysis.ProtoTypeSystem.Types;

import edu.clemson.resolve.analysis.ProtoTypeSystem.SymbolTable.SymbolTableEntry;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class MTFunction extends MTHyperSet {
    private List<MTEntity> myDomainTypes;
    private MTEntity myRangeType;
    private TypeFactory myApplicationTypeFactory;

    public MTFunction(String canonicalName, MTEntity domainType, MTEntity rangeType) {
        super(canonicalName);
        myDomainTypes = new LinkedList<>();
        myDomainTypes.add(domainType);
        myRangeType = rangeType;
    }

    public MTFunction(String canonicalName, MTEntity domainType, MTEntity rangeType, TypeFactory factory) {
        this(canonicalName, domainType, rangeType);
        myApplicationTypeFactory = factory;
    }

    public MTFunction(String canonicalName, List<MTEntity> domainTypes, MTEntity rangeType) {
        super(canonicalName);
        myDomainTypes = new LinkedList<>(domainTypes);
        myRangeType = rangeType;
    }

    public List<MTEntity> getDomainTypes() {
        return myDomainTypes;
    }

    public MTEntity getRangeType() {
        return myRangeType;
    }

    public TypeFactory getApplicationTypeFactory() {
        return myApplicationTypeFactory;
    }

    public MTFunctionApplication getFunctionApplicationType(List<SymbolTableEntry> arguments) {
        return new MTFunctionApplication(this, arguments);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (myDomainTypes.size() == 1) {
            sb.append(myDomainTypes.get(0));
        } else {
            sb.append("( ");
            Iterator<MTEntity> iter = myDomainTypes.iterator();
            while (iter.hasNext()) {
                sb.append(iter.next());
                if (iter.hasNext()) {
                    sb.append(" x ");
                }
            }
            sb.append(" )");
        }
        sb.append(" -> ");
        sb.append(myRangeType);
        return sb.toString();
    }
}
