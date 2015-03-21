package resolvelite.typereasoning;

import org.antlr.v4.runtime.misc.NotNull;
import resolvelite.compiler.ResolveCompiler;
import resolvelite.semantics.MathTypePowersetApplication;
import resolvelite.semantics.MathType;
import resolvelite.semantics.MathTypeFunc;
import resolvelite.semantics.MathTypeFunc.MathTypeFuncBuilder;
import resolvelite.semantics.MathTypeFuncApplication.FunctionApplicationFactory;
import resolvelite.semantics.MathTypeProp;

import java.util.List;

public class TypeGraph {

    public final MathTypeProp CLS = new MathTypeProp(this, null, true, "Cls");
    public final MathTypeProp SSET = new MathTypeProp(this, CLS, true, "SSet");
    public final MathTypeProp BOOLEAN =
            new MathTypeProp(this, SSET, false, "B");
    public final MathTypeProp VOID = new MathTypeProp(this, CLS, false, "Void");

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
    }
}
