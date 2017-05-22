package edu.clemson.resolve.analysis.ProtoTypeSystem.Types;

public class MTClass extends MTHyperSet {
    public static final TypeFactory FACTORY = (String canonicalName, MTEntity knownSupertype) ->  new MTClass(canonicalName);

    public MTClass(String canonicalName) {
        super(canonicalName);
    }

    public MTClass(String canonicalName, MTEntity knownSupertype) {
        super(canonicalName, knownSupertype);
    }

    public MTClass(String canonicalName, TypeFactory factory) {
        super(canonicalName, factory);
    }

    @Override
    public boolean isEquivalentOrSubtypeOf(MTEntity type) {
        if (super.isEquivalentOrSubtypeOf(type)) {
            return true;
        }

        if (type == CLS) {
            return true;
        }

        return false;
    }
}
