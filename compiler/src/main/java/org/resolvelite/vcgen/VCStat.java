package org.resolvelite.vcgen;

import org.antlr.v4.runtime.misc.NotNull;
import org.resolvelite.codegen.model.OutputModelObject;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PExp;

public abstract class VCStat<T> extends OutputModelObject {

    private final T contents;

    public VCStat(@NotNull T contents) {
        this.contents = contents;
    }

    public T getAssertion() {
        return contents;
    }

    public static class VCConfirm extends VCStat<PExp> {
        public VCConfirm(@NotNull PExp assertion) {
            super(assertion);
        }
    }

    public static class VCAssume extends VCStat<PExp> {
        public VCAssume(@NotNull PExp contents) {
            super(contents);
        }
    }

    public static class VCVars
            extends
                VCStat<ResolveParser.VariableDeclGroupContext> {
        public VCVars(
                @NotNull ResolveParser.VariableDeclGroupContext contents) {
            super(contents);
        }
    }


}
