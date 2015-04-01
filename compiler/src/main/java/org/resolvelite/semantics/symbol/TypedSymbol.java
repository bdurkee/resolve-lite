package org.resolvelite.semantics.symbol;

import org.resolvelite.semantics.Type;

public interface TypedSymbol {
    public Type getType();

    public void setType(Type t);
}
