package edu.clemson.resolve.analysis.ProtoTypeSystem.Types;

public interface TypeFactory {
    MTEntity createElementType(String canonicalName, MTEntity knownSupertype);
}
