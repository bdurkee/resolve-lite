package org.resolvelite.typereasoning;

import org.antlr.v4.runtime.misc.NotNull;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.semantics.*;

import java.util.List;

public class TypeGraph {
    public final MathTypeProper CLS =
            new MathTypeProper(this, null, true, "Cls");
    public final MathTypeProper SSET =
            new MathTypeProper(this, CLS, true, "SSet");
    public final MathTypeProper BOOLEAN =
            new MathTypeProper(this, SSET, false, "B");
    public final MathTypeProper EMPTY_SET =
            new MathTypeProper(this, SSET, false, "Empty_Set");
}
