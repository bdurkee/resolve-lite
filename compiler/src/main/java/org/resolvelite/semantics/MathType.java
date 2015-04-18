package org.resolvelite.semantics;

import org.resolvelite.typereasoning.TypeGraph;

public abstract class MathType {

    private final TypeGraph typeGraph;

    public MathType(TypeGraph g) {
        this.typeGraph = g;
    }

    public TypeGraph getTypeGraph() {
        return typeGraph;
    }

    /**
     * Indicates that this type is known to contain only elements <em>that
     * are themselves</em> sets. Practically, this answers the question, "can
     * an instance of this type itself be used as a type?"
     */
    public boolean isKnownToContainOnlySets() {
        return false;
    }
}
