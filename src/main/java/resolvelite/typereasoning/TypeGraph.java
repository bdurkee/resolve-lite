package resolvelite.typereasoning;

import org.antlr.v4.runtime.misc.NotNull;
import resolvelite.compiler.ResolveCompiler;
import resolvelite.semantics.MTProper;

public class TypeGraph {

    private final ResolveCompiler compiler;

    public TypeGraph(@NotNull ResolveCompiler rc) {
        this.compiler = rc;
    }

    public final MTProper CLS = new MTProper(this, null, true, "Cls");
    public final MTProper SSET = new MTProper(this, CLS, true, "SSet");
    public final MTProper BOOLEAN = new MTProper(this, SSET, false, "B");

}
