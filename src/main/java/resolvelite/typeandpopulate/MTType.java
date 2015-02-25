package resolvelite.typeandpopulate;

import resolvelite.typereasoning.TypeGraph;

public abstract class MTType {

    protected final TypeGraph myTypeGraph;

    /**
     * <p>Allows us to detect if we're getting into an equals-loop.</p>
     */
    private int myEqualsDepth = 0;

    public MTType(TypeGraph typeGraph) {
        myTypeGraph = typeGraph;
    }

    public TypeGraph getTypeGraph() {
        return myTypeGraph;
    }

    /**
     * <p>Indicates that this type is known to contain only elements <em>that
     * are themselves</em> types.  Practically, this answers the question, "can
     * an instance of this type itself be used as a type?"</p>
     */
    public boolean isKnownToContainOnlyMTypes() {
        return false;
    }

    /**
     * <p>Indicates that every instance of this type is itself known to contain
     * only elements that are types.  Practically, this answers the question,
     * "if a function returns an instance of this type, can that instance itself
     * be said to contain only types?"</p>
     */
    public boolean membersKnownToContainOnlyMTypes() {
        return false;
    }
}
