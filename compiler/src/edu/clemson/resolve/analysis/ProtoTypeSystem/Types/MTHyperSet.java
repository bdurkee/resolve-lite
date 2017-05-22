package edu.clemson.resolve.analysis.ProtoTypeSystem.Types;

public class MTHyperSet extends MTEntity {
    public static final TypeFactory FACTORY = (String canonicalName, MTEntity knownSupertype) ->  new MTHyperSet(canonicalName);

    public MTHyperSet() {
        super();
    }

    public MTHyperSet(String canonicalName) {
        super(canonicalName);
    }

    public MTHyperSet(String canonicalName, MTEntity knownSupertype) {
        super(canonicalName, knownSupertype);
    }

    public MTHyperSet(String canonicalName, TypeFactory factory) {
        super(canonicalName, factory);
    }

    @Override
    public boolean isKnownType() {
        return true;
    }
}
