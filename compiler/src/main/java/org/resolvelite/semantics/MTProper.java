package org.resolvelite.semantics;

import org.resolvelite.typereasoning.TypeGraph;

import java.util.Collections;
import java.util.List;

/**
 * A proper type. Any type that does not depend on other types. I.e., it
 * is atomic.
 */
public class MTProper extends MTType {

    private String name;
    private MTType type = null;
    private final boolean knownToContainOnlyMTypesFlag;

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
        this.knownToContainOnlyMTypesFlag = knownToContainOnlyMTypes;
        this.type = type;
        this.name = name;
    }

    @Override public boolean isKnownToContainOnlyMTypes() {
        return knownToContainOnlyMTypesFlag;
    }

    @Override public List<MTType> getComponentTypes() {
        return Collections.emptyList();
    }

    public String getName() {
        return name;
    }

    @Override public MTType getType() {
        return type;
    }

    @Override public String toString() {
        String result;

        if ( name == null ) {
            result = super.toString();
        }
        else {
            result = name;
        }
        return result;
    }

    @Override public MTType withComponentReplaced(int index, MTType newType) {
        throw new IndexOutOfBoundsException();
    }

    @Override public void acceptOpen(TypeVisitor v) {
        v.beginMTType(this);
        v.beginMTProper(this);
    }

    @Override public void accept(TypeVisitor v) {
        acceptOpen(v);

        v.beginChildren(this);
        v.endChildren(this);

        acceptClose(v);
    }

    @Override public void acceptClose(TypeVisitor v) {
        v.endMTProper(this);
        v.endMTType(this);
    }

    @Override public int getHashCode() {
        return objectReferenceHashCode();
    }

}