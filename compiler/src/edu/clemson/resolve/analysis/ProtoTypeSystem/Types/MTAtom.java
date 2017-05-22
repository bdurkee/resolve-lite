package edu.clemson.resolve.analysis.ProtoTypeSystem.Types;

public class MTAtom extends MTEntity {
    public static final TypeFactory FACTORY = (String canonicalName, MTEntity knownSupertype) ->  new MTAtom(canonicalName);

    public MTAtom(String canonicalName) {
        super(canonicalName);
    }
}
