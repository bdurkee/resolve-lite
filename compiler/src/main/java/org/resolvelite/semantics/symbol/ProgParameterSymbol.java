package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.tree.ParseTree;

public class ProgParameterSymbol extends Symbol {

    public ProgParameterSymbol(String name, ParseTree definingTree,
            String moduleID) {
        super(name, definingTree, moduleID);
    }
}
