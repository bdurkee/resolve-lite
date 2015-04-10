package org.resolvelite.typereasoning;

import org.antlr.v4.runtime.misc.NotNull;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.semantics.MTProper;

public class TypeGraph {

    public final MTProper CLS = new MTProper(this, null, true,
            "Cls");
    public final MTProper SSET = new MTProper(this, CLS, true,
            "SSet");
    public final MTProper BOOLEAN = new MTProper(this, SSET, false,
            "B");
    public final MTProper VOID = new MTProper(this, CLS, false,
            "Void");
    public final MTProper EMPTY_SET = new MTProper(this, SSET,
            false, "Empty_Set");

    /* public final MathTypeFunc POWERSET = //
             new MathTypeFuncBuilder(this, POWERSET_APPLICATION, SSET) //
                     .paramTypes(SSET) //
                     .elementsRestrict(true).build();

     private final static FunctionApplicationFactory POWERSET_APPLICATION =
             new PowertypeApplicationFactory();*/

    private final ResolveCompiler compiler;

    public TypeGraph(@NotNull ResolveCompiler rc) {
        this.compiler = rc;
    }

    /* private static class PowertypeApplicationFactory
             implements
                 FunctionApplicationFactory {

         @Override
         public MathType buildFunctionApplication(TypeGraph g, MathTypeFunc f,
                 String refName, List<MathType> args) {
             return new MathTypePowersetApplication(g, args.get(0));
         }
     }*/
}
