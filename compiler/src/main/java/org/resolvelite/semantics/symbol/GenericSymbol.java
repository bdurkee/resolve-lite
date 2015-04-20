package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.tree.ParseTree;

public class GenericSymbol extends Symbol {

    public GenericSymbol(String name, ParseTree definingTree, String moduleID) {
        super(name, definingTree, moduleID);
    }

    @Override public String getEntryTypeDescription() {
        return "a generic";
    }

    @Override public GenericSymbol toGenericSymbol() {
        return this;
    }
}
