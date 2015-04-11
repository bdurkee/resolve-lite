package org.resolvelite.semantics;

import org.resolvelite.typereasoning.TypeGraph;

import java.util.Collections;
import java.util.List;

public class MTProper extends MTType {
    private String name;
    private MTType type = null;
    private final boolean knownToContainOnlyMathTypesFlag;

    public MTProper(TypeGraph g) {
        this(g, null, false, null);
    }

    public MTProper(TypeGraph g, boolean knownToContainOnlyMTypes) {
        this(g, null, knownToContainOnlyMTypes, null);
    }

    public MTProper(TypeGraph g, String name) {
        this(g, null, false, name);
    }

    public MTProper(TypeGraph g, MTType type, boolean knownToContainOnlyMTypes,
            String name) {
        super(g);
        this.knownToContainOnlyMathTypesFlag = knownToContainOnlyMTypes;
        this.type = type;
        this.name = name;
    }

    @Override public List<MTType> getComponentTypes() {
        return Collections.emptyList();
    }

    @Override public boolean isKnownToContainOnlyMathTypes() {
        return knownToContainOnlyMathTypesFlag;
    }

    public String getName() {
        return name;
    }

    public MTType getType() {
        return type;
    }
}
