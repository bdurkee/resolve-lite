package org.resolvelite.semantics.programtype;

import org.resolvelite.semantics.MTType;
import org.resolvelite.typereasoning.TypeGraph;

/**
 * Created by daniel on 4/18/15.
 */
public class PTFamily extends PTType {
    public PTFamily(TypeGraph g) {
        super(g);
    }

    @Override public MTType toMath() {
        return null;
    }
}
