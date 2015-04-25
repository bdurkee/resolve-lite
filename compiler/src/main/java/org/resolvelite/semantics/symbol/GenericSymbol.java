package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.typereasoning.TypeGraph;

public class GenericSymbol extends Symbol {

    private MathSymbol mathSymbolAlterEgo;

    public GenericSymbol(TypeGraph g, String name, ParseTree definingTree,
            String moduleID) {
        super(name, definingTree, moduleID);
    }

    @Override public String getEntryTypeDescription() {
        return "a generic";
    }

    @Override public GenericSymbol toGenericSymbol() {
        return this;
    }
}
