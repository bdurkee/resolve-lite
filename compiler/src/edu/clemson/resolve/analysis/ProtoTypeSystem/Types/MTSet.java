package edu.clemson.resolve.analysis.ProtoTypeSystem.Types;

public class MTSet extends MTClass {
    public static final TypeFactory FACTORY = (String canonicalName, MTEntity knownSupertype) ->  new MTSet(canonicalName);

    public MTSet(String canonicalName) {
        super(canonicalName);
    }

    public MTSet(String canonicalName, MTEntity knownSupertype) {
        super(canonicalName, knownSupertype);
    }

    public MTSet(String canonicalName, TypeFactory factory) {
        super(canonicalName, factory);
    }

    @Override
    public boolean isEquivalentOrSubtypeOf(MTEntity type) {
        if (super.isEquivalentOrSubtypeOf(type)) {
            return true;
        }

        if (type == SSET || type == CLS) {
            return true;
        }

        return false;
    }
}
