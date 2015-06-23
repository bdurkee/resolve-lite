package org.rsrg.semantics;

import edu.clemson.resolve.typereasoning.TypeGraph;

public abstract class MTAbstract<T extends MTType> extends MTType {

    public MTAbstract(TypeGraph typeGraph) {
        super(typeGraph);
    }
}
