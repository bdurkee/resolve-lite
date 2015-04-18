package org.resolvelite.semantics.symbol;

public class MathSymbol extends AbstractMathSymbol {

    MathType parentType;

    public MathSymbol(String name, MathType parentType,
                      boolean knownToContainOnlySets, String rootModuleID) {
        super(name, knownToContainOnlySets, rootModuleID);
        this.parentType = parentType;
    }

    public MathSymbol(String name,
                      String rootModuleID) {
        super(name, false, rootModuleID);
    }



}
