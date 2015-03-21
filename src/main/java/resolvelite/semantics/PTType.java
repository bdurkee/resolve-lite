package resolvelite.semantics;

import resolvelite.typereasoning.TypeGraph;

public abstract class PTType {

    private final TypeGraph myTypeGraph;

    public PTType(TypeGraph g) {
        myTypeGraph = g;
    }

    public final TypeGraph getTypeGraph() {
        return myTypeGraph;
    }

    public abstract MTType toMath();

    /**
     * Returns <code>true</code> <strong>iff</strong> a value of this type
     * is acceptable where one of type <code>t</code> is required.
     * 
     * @param t The required type.
     * 
     * @return <code>true</code> <strong>iff</strong> an value of this type
     *         would be acceptable where one of type <code>t</code> were
     *         required.
     */
    public boolean acceptableFor(PTType t) {
        return equals(t);
    }
}
