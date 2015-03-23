package resolvelite.semantics.symbol;

import resolvelite.semantics.Type;

public interface TypedSymbol {
    public Type getType();

    public void setType(Type t);
}
