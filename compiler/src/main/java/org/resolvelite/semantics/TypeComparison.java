package org.resolvelite.semantics;

import org.resolvelite.proving.absyn.PExp;

public interface TypeComparison<V extends PExp, T extends MTType> {

    public boolean compare(V foundValue, T foundType, T expectedType);

    public String description();
}
