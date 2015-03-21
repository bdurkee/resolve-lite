package resolvelite.semantics;

import resolvelite.typereasoning.TypeGraph;

public abstract class MathType {

    protected final TypeGraph typeGraph;

    public MathType(TypeGraph typeGraph) {
        this.typeGraph = typeGraph;
    }

    public TypeGraph getTypeGraph() {
        return typeGraph;
    }

    /**
     * Indicates that this type is known to contain only elements <em>that
     * are themselves</em> types. Practically, this answers the question, "can
     * an instance of this type itself be used as a type?"?
     */
    public boolean isKnownToContainOnlyThingsThatAreTypes() {
        return false;
    }

    /**
     * Indicates that every instance of this type is itself known to contain
     * only elements that are types. Practically, this answers the question,
     * "if a function returns an instance of this type, can that instance itself
     * be said to contain only types?"
     */
    public boolean membersKnownToContainOnlyThingsThatAreTypes() {
        return false;
    }
}
