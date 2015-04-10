package org.resolvelite.semantics;

import org.resolvelite.misc.Utils.Builder;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.List;

public class MTFunction extends MTType {

    private static final FunctionApplicationFactory DEFAULT_FACTORY =
            new VanillaFunctionApplicationFactory();

    private final MTType domain, range;
    private final boolean restrictionFlag;
    private final FunctionApplicationFactory functionApplicationFactory;

    private List<MTType> components;

    private MTFunction(MTFunctionBuilder builder) {
        super(builder.g);
        this.range = builder.range;
    }

    public static class MTFunctionBuilder implements Builder<MTFunction> {
        protected final TypeGraph g;
        protected final MTType range;
        protected MTType domain;

        public MTFunctionBuilder(TypeGraph g, MTType range) {
            this.g = g;
            this.range = range;
        }

        @Override public MTFunction build() {
            return new MTFunction(this);
        }
    }

    private static class VanillaFunctionApplicationFactory
            implements
            FunctionApplicationFactory {

        @Override
        public MTType buildFunctionApplication(TypeGraph g, MTFunction f,
                                               String calledAsName, List<MTType> arguments) {
            return new MTFunctionApplication(g, f, calledAsName, arguments);
        }
    }

    public MTType getDomain() {
        return domain;
    }

    public MTType getRange() {
        return range;
    }

}
