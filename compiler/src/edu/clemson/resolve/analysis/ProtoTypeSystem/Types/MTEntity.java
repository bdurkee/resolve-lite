package edu.clemson.resolve.analysis.ProtoTypeSystem.Types;

import java.util.HashSet;
import java.util.Set;

public class MTEntity {
    public static final TypeFactory FACTORY = (String canonicalName, MTEntity knownSupertype) -> new MTEntity(canonicalName);

    public static final MTEntity HYPERSET = new MTEntity("HyperSet", MTHyperSet.FACTORY);
    public static final MTHyperSet CLS = new MTHyperSet("CLS", MTClass.FACTORY);
    public static final MTClass SSET = new MTClass("SSET", MTSet.FACTORY);
    public static final MTSet BOOLEAN = new MTSet(("B"), MTAtom.FACTORY);

    public static final MTFunction POWERCLASS = new MTFunction("Powerclass", CLS, CLS, MTFunctionApplication.POWERCLASS_FACTORY);
    public static final MTFunction POWERSET = new MTFunction("Powerset", SSET, SSET, MTFunctionApplication.POWERSET_FACTORY);


    // ********* CLASS MEMBERS *********

    protected String myCanonicalName;
    protected Set<String> myKnownAliases;
    protected Set<MTEntity> myKnownSupertypes;
    protected TypeFactory myTypeFactory;

    public MTEntity() {
        myCanonicalName = "[AnonType]";
        myKnownAliases = new HashSet<>();
        myKnownSupertypes = new HashSet<>();
        myTypeFactory = MTEntity.FACTORY;
    }

    public MTEntity(String canonicalName) {
        this();
        myCanonicalName = canonicalName;
    }

    public MTEntity(String canonicalName, MTEntity knownSupertype) {
        this(canonicalName);
        if (knownSupertype != null) {
            myKnownSupertypes.add(knownSupertype);
        }
    }

    public MTEntity(String canonicalName, TypeFactory factory) {
        this(canonicalName);
        myTypeFactory = factory;
    }

    public String getCanonicalName() {
        return myCanonicalName;
    }

    public MTEntity createElementType(String canonicalName) {
        return myTypeFactory.createElementType(canonicalName, null);
    }

    public boolean isKnownType() {
        return false;
    }

    public void addKnownSupertype(MTEntity supertype) {
        myKnownSupertypes.add(supertype);
    }

    public boolean isEquivalentOrSubtypeOf(MTEntity type) {
        if (this == type) {
            return true;
        }

        for (MTEntity supertype : myKnownSupertypes) {
            if (supertype.isEquivalentOrSubtypeOf(type)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return myCanonicalName;
    }
}
