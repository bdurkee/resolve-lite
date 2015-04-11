package org.resolvelite.typereasoning;

import org.antlr.v4.runtime.misc.NotNull;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.semantics.*;

import java.util.List;

public class TypeGraph {

    public final MTProper CLS = new MTProper(this, null, true, "Cls");
    public final MTProper SSET = new MTProper(this, CLS, true, "SSet");
    public final MTProper BOOLEAN = new MTProper(this, SSET, false, "B");
    public final MTProper VOID = new MTProper(this, CLS, false, "Void");
    public final MTProper EMPTY_SET = new MTProper(this, SSET, false,
            "Empty_Set");

    public final MTFunction POWERSET = //
            new MTFunction.MTFunctionBuilder(this, POWERSET_APPLICATION, SSET) //
                    .paramTypes(SSET) //
                    .elementsRestrict(true).build();

    private final static FunctionApplicationFactory POWERSET_APPLICATION =
            new PowersetApplicationFactory();

    private static class PowersetApplicationFactory
            implements
                FunctionApplicationFactory {

        @Override public MTType buildFunctionApplication(TypeGraph g,
                MTFunction f, String refName, List<MTType> args) {
            return new MTPowersetApplication(g, args.get(0));
        }
    }
}
