package org.resolvelite.semantics.symbol;

import org.resolvelite.semantics.MathType;

public class MathSymbol extends BaseSymbol implements MathTypedSymbol {

    public MathType type;
    private String rootModule;

    public MathSymbol(String name, MathType type, String rootModuleID) {
        super(name, rootModuleID);
        this.setMathType(type);
    }

    public MathSymbol(String name, String rootModuleID) {
        super(name, rootModuleID);
    }

    @Override public String getName() {
        return name;
    }

    @Override public String getRootModuleID() {
        return rootModule;
    }

    @Override
    public MathType getMathType() {
        return type;
    }

    @Override
    public void setMathType(MathType t) {
        this.type = t;
    }
}
