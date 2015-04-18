package org.resolvelite.semantics.symbol;

public abstract class AbstractMathSymbol extends BaseSymbol implements MathType {

    private boolean knownToContainOnlySets;

    public AbstractMathSymbol(String name, boolean knownToContainOnlySets,
                              String rootModuleID) {
        super(name, rootModuleID);
        this.knownToContainOnlySets = knownToContainOnlySets;
    }

    @Override public boolean knownToContainOnlySets() {
        return false;
    }

    @Override public boolean membersKnownToContainOnlySets() {
        return false;
    }
}
