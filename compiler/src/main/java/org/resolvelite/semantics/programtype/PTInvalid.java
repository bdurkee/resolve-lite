package org.resolvelite.semantics.programtype;

import org.resolvelite.semantics.MTType;
import org.resolvelite.typereasoning.TypeGraph;

public class PTInvalid extends PTType {

    public PTInvalid(TypeGraph g) {
        super(g);
    }

    @Override public MTType toMath() {
        return null;
    }
}
