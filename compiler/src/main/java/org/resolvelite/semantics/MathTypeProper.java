package org.resolvelite.semantics;

import org.resolvelite.typereasoning.TypeGraph;

public class MathTypeProper extends MathType {

    private String name;
    private MathType type = null;
    private final boolean knownToContainOnlyMathTypesFlag;

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
                          boolean knownToContainOnlyMTypes,
                    String name) {
        super(g);
        this.knownToContainOnlyMathTypesFlag = knownToContainOnlyMTypes;
        this.type = type;
        this.name = name;
    }

    @Override
    public boolean isKnownToContainOnlyMathTypes() {
        return knownToContainOnlyMathTypesFlag;
    }

    public String getName() {
        return name;
    }

    public MathType getType() {
        return type;
    }
}
