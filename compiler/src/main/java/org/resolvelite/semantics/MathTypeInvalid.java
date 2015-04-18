package org.resolvelite.semantics;

import org.resolvelite.typereasoning.TypeGraph;

public class MathTypeInvalid extends MathType {

    public MathTypeInvalid(TypeGraph g) {
        super(g);
    }

    public String getName() {
        return "Invalid math type";
    }
}
