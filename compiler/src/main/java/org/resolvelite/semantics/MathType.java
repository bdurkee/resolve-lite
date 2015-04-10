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
     * <p>Indicates that this type is known to contain only elements <em>that
     * are themselves</em> types.  Practically, this answers the question, "can
     * an instance of this type itself be used as a type?"</p>
     */
    public boolean isKnownToContainOnlyMathTypes() {
        return false;
    }

    /**
     * <p>Indicates that every instance of this type is itself known to contain
     * only elements that are types.  Practically, this answers the question,
     * "if a function returns an instance of this type, can that instance itself
     * be said to contain only types?"</p>
     */
    public boolean membersKnownToContainOnlyMathTypes() {
        return false;
    }
}
