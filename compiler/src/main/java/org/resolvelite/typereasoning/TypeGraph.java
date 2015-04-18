package org.resolvelite.typereasoning;

import org.resolvelite.semantics.MathTypeProper;
import org.resolvelite.semantics.symbol.MathSymbol;

public class TypeGraph {

    public final MathTypeProper CLS =
            new MathTypeProper(this, null, true, "Cls");
    public final MathTypeProper SSET =
            new MathTypeProper(this, CLS, true, "SSet");
    public final MathTypeProper BOOLEAN =
            new MathTypeProper(this, SSET, false, "B");
}
