package org.resolvelite.semantics.symbol;

import org.resolvelite.semantics.MathType;
import org.resolvelite.semantics.Type;

public class MathSymbol extends BaseSymbol implements MathType {

    private boolean knownToContainOnlySets;
    private String rootModule;

    public MathSymbol(String name, boolean containsOnlySets,
            String rootModuleID) {
        super(name, rootModuleID);
        this.knownToContainOnlySets = containsOnlySets;
    }

    @Override public String getName() {
        return name;
    }

    @Override public String getRootModuleID() {
        return rootModule;
    }

    @Override public boolean isKnownToContainOnlySets() {
        return knownToContainOnlySets;
    }

    @Override public MathSymbol getType() {
        return this;
    }

}
