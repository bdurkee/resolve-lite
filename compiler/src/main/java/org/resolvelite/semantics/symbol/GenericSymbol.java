package org.resolvelite.semantics.symbol;

import org.resolvelite.semantics.Type;

public class GenericSymbol extends BaseSymbol implements Type {

    public GenericSymbol(String name, String rootModuleID) {
        super(name, rootModuleID);
    }
}
