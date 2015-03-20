package resolvelite.semantics;

import org.antlr.v4.runtime.misc.NotNull;
import resolvelite.typereasoning.TypeGraph;

/**
 * A proper type. Any type that does not depend on other types. I.e., it
 * is atomic.
 */
public class MTProper extends MTType {

    private String name;
    private MTType type = null;
    private final boolean knownToContainOnlyMTypes;

    public MTProper(@NotNull TypeGraph g) {
        this(g, null, false, null);
    }

    public MTProper(@NotNull TypeGraph g, boolean knownToContainOnlyMTypes) {
        this(g, null, knownToContainOnlyMTypes, null);
    }

    public MTProper(@NotNull TypeGraph g, String name) {
        this(g, null, false, name);
    }

    public MTProper(@NotNull TypeGraph g, MTType type,
            boolean knownToContainOnlyMTypes, String name) {
        super(g);
        this.knownToContainOnlyMTypes = knownToContainOnlyMTypes;
        this.type = type;
        this.name = name;
    }

    @Override
    public boolean isKnownToContainOnlyMTypes() {
        return knownToContainOnlyMTypes;
    }

    public String getName() {
        return name;
    }

    public MTType getType() {
        return type;
    }

    @Override
    public String toString() {
        return getName();
    }
}
