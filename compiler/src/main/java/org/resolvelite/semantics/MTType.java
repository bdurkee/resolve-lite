package org.resolvelite.semantics;

import org.resolvelite.typereasoning.TypeGraph;

import java.util.List;

public abstract class MTType {

    private final TypeGraph typeGraph;

    public MTType(TypeGraph typeGraph) {
        this.typeGraph = typeGraph;
    }

    public TypeGraph getTypeGraph() {
        return typeGraph;
    }

    public abstract List<? extends MTType> getComponentTypes();

    /**
     * Indicates that this type is known to contain only elements <em>that
     * are themselves</em> types. Practically, this answers the question, "can
     * an instance of this type itself be used as a type?"
     */
    public boolean isKnownToContainOnlyMathTypes() {
        return false;
    }

    /**
     * Indicates that every instance of this type is itself known to contain
     * only elements that are types. Practically, this answers the question,
     * "if a function returns an instance of this type, can that instance itself
     * be said to contain only types?"
     */
    public boolean membersKnownToContainOnlyMTypes() {
        return false;
    }
}
