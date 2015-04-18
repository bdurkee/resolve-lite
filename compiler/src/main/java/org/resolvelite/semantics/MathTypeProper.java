package org.resolvelite.semantics;

import org.resolvelite.typereasoning.TypeGraph;

public class MathTypeProper extends MathType {

    public boolean knownToContainOnlySets;
    public String name;
    public MathType type;

    public MathTypeProper(TypeGraph g) {
        this(g, null, false, null);
    }

    public MathTypeProper(TypeGraph g, boolean knownToContainOnlyMTypes) {
        this(g, null, knownToContainOnlyMTypes, null);
    }

    public MathTypeProper(TypeGraph g, String name) {
        this(g, null, false, name);
    }

    public MathTypeProper(TypeGraph g, MathType type,
              boolean knownToContainOnlySets, String name) {
        super(g);
        this.knownToContainOnlySets = knownToContainOnlySets;
        this.type = type;
        this.name = name;
    }

    @Override public boolean isKnownToContainOnlySets() {
        return knownToContainOnlySets;
    }
}
