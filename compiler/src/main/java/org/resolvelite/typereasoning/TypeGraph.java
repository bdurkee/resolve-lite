package org.resolvelite.typereasoning;

public class TypeGraph {

    /* public final MathTypeProper CLS = new MathTypeProper(this, null, true,
             "Cls");
     public final MathTypeProper SSET = new MathTypeProper(this, CLS, true,
             "SSet");
     public final MathTypeProper BOOLEAN = new MathTypeProper(this, SSET, false,
             "B");
     public final MathTypeProper VOID = new MathTypeProper(this, CLS, false,
             "Void");
     public final MathTypeProper EMPTY_SET = new MathTypeProper(this, SSET,
             false, "Empty_Set");

     public final MathTypeFunc POWERSET = //
             new MathTypeFuncBuilder(this, POWERSET_APPLICATION, SSET) //
                     .paramTypes(SSET) //
                     .elementsRestrict(true).build();

     private final static FunctionApplicationFactory POWERSET_APPLICATION =
             new PowertypeApplicationFactory();

     private final ResolveCompiler compiler;

     public TypeGraph(@NotNull ResolveCompiler rc) {
         this.compiler = rc;
     }

     private static class PowertypeApplicationFactory
             implements
                 FunctionApplicationFactory {

         @Override
         public MathType buildFunctionApplication(TypeGraph g, MathTypeFunc f,
                 String refName, List<MathType> args) {
             return new MathTypePowersetApplication(g, args.get(0));
         }
     }*/
}
