package org.resolvelite.semantics;

public interface MathType extends Type {

    //Can this actually be used as a type?
    public boolean isKnownToContainOnlySets();
}
