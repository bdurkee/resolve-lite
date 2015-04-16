package org.resolvelite.semantics;

import org.resolvelite.typereasoning.TypeGraph;

import java.util.Collections;
import java.util.List;

public class MathTypeProper extends MathType {

    private String name;
    private MathType type;
    private final boolean knownToContainOnlyMathTypesFlag;

    public MathTypeProper(TypeGraph g, MathType type,
                          boolean knownToContainOnlyMathTypesFlag, String name) {
        super(g);
        this.name = name;
        this.type = type;
        this.knownToContainOnlyMathTypesFlag = knownToContainOnlyMathTypesFlag;
    }

    @Override public List<MathType> getComponentTypes() {
        return Collections.emptyList();
    }

    @Override public boolean isKnownToContainOnlyMathTypes() {
        return knownToContainOnlyMathTypesFlag;
    }

    public String getName() {
        return name;
    }
}
