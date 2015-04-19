package org.resolvelite.semantics;

import org.resolvelite.typereasoning.TypeGraph;

import java.util.Collections;
import java.util.List;

/**
 * A proper type. Any type that does not depend on other types. I.e., it
 * is atomic.
 */
public class MTProper extends MTType {

    private String myName;
    private MTType myType = null;
    private final boolean myKnownToContainOnlyMTypesFlag;

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
        myKnownToContainOnlyMTypesFlag = knownToContainOnlyMTypes;
        myType = type;
        myName = name;
    }

    @Override public boolean isKnownToContainOnlyMTypes() {
        return myKnownToContainOnlyMTypesFlag;
    }

    public String getName() {
        return myName;
    }

    public MTType getType() {
        return myType;
    }

    @Override public String toString() {
        String result;

        if ( myName == null ) {
            result = super.toString();
        }
        else {
            result = myName;
        }
        return result;
    }

}