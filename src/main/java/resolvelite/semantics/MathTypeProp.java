package resolvelite.semantics;

import org.antlr.v4.runtime.misc.NotNull;
import resolvelite.typereasoning.TypeGraph;

/**
 * A proper type. Any type that does not depend on other types. I.e., it
 * is atomic.
 */
public class MathTypeProp extends MathType {

    private String name;
    private MathType type = null;
    private final boolean knownToContainOnlyThingsThatAreTypes;

    public MathTypeProp(@NotNull TypeGraph g) {
        this(g, null, false, null);
    }

    public MathTypeProp(@NotNull TypeGraph g, boolean knownToContainOnlyMTypes) {
        this(g, null, knownToContainOnlyMTypes, null);
    }

    public MathTypeProp(@NotNull TypeGraph g, String name) {
        this(g, null, false, name);
    }

    public MathTypeProp(@NotNull TypeGraph g, MathType type,
                        boolean knownToContainOnlyTypes, String name) {
        super(g);
        this.knownToContainOnlyThingsThatAreTypes = knownToContainOnlyTypes;
        this.type = type;
        this.name = name;
    }

    @Override
    public boolean isKnownToContainOnlyThingsThatAreTypes() {
        return knownToContainOnlyThingsThatAreTypes;
    }

    public String getName() {
        return name;
    }

    public MathType getType() {
        return type;
    }

    @Override
    public String toString() {
        return getName();
    }
}
