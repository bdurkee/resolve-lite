package org.rsrg.semantics;

import edu.clemson.resolve.proving.absyn.PExp;

public interface TypeComparison<V extends PExp, T extends MTType> {

    public boolean compare(V foundValue, T foundType, T expectedType);

    public String description();
}
