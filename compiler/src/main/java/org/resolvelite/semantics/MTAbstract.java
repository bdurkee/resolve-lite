package org.resolvelite.semantics;

import org.resolvelite.typereasoning.TypeGraph;

public abstract class MTAbstract<T extends MTType> extends MTType {

    public MTAbstract(TypeGraph typeGraph) {
        super(typeGraph);
    }
}
