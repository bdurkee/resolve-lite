package resolvelite.semantics;

import org.antlr.v4.runtime.misc.NotNull;
import resolvelite.misc.Utils;
import resolvelite.typereasoning.TypeGraph;
import resolvelite.semantics.MathTypeFuncApplication.FunctionApplicationFactory;

import java.util.*;

public class MathTypeFunc extends MathType {

    private static final FunctionApplicationFactory DEFAULT_FACTORY =
            new VanillaFunctionApplicationFactory();

    private final FunctionApplicationFactory applyFactory;
    private final MathType domain, range;
    private final boolean restricts;

    private MathTypeFunc(@NotNull MathTypeFuncBuilder builder) {
        super(builder.typeGraph);
        this.domain = builder.domain;
        this.range = builder.range;
        this.restricts = builder.elementsRestrict;
        this.applyFactory = builder.factory;
    }

    @NotNull
    public MathType getDomain() {
        return domain;
    }

    @NotNull
    public MathType getRange() {
        return range;
    }

    @Override
    public boolean isKnownToContainOnlyThingsThatAreTypes() {
        return false;
    }

    public boolean applicationResultsKnownToContainOnlyRestrictions() {
        return restricts;
    }

    private static List<String> buildNullNameListOfEqualLength(MathType... t) {
        return buildNullNameListOfEqualLength(Arrays.asList(t));
    }

    private static List<String> buildNullNameListOfEqualLength(
            List<MathType> original) {
        List<String> names = new ArrayList<>();

        for (@SuppressWarnings("unused") MathType t : original) {
            names.add(null);
        }
        return names;
    }

    @NotNull
    public MathType getApplicationType(String refName, MathTypeFunc f,
            List<MathType> args) {
        return applyFactory.buildFunctionApplication(typeGraph, f, refName,
                args);
    }

    public static MathType buildParameterType(TypeGraph g,
            List<String> paramNames, List<MathType> paramTypes) {
        MathType result;

        switch (paramTypes.size()) {
        case 0:
            result = g.VOID;
            break;
        case 1:
            result = paramTypes.get(0);
            break;
        default:
            List<MathTypeCartProd.Element> elements = new ArrayList<>();
            Iterator<String> namesIter = paramNames.iterator();
            Iterator<MathType> typesIter = paramTypes.iterator();
            while (namesIter.hasNext()) {
                elements.add(new MathTypeCartProd.Element(typesIter.next(),
                        namesIter.next()));
            }
            result = new MathTypeCartProd(g, elements);
        }
        return result;
    }

    @Override
    public String toString() {
        return "(" + domain.toString() + "->" + range.toString() + ")";
    }

    /**
     * Allows users to construct instances of {@link MathTypeFunc} piecemeal
     * while avoiding extremely long, confusing telescoping constructors calls.
     */
    private static class VanillaFunctionApplicationFactory
            implements
                FunctionApplicationFactory {

        @Override
        @NotNull
        public MathType buildFunctionApplication(@NotNull TypeGraph g,
                @NotNull MathTypeFunc f, @NotNull String refName,
                List<MathType> args) {
            return new MathTypeFuncApplication(g, f, refName, args);
        }
    }

    public static class MathTypeFuncBuilder
            implements
                Utils.Builder<MathTypeFunc> {

        private final List<String> paramNames = new ArrayList<>();
        private final List<MathType> paramTypes = new ArrayList<>();

        protected final TypeGraph typeGraph;
        protected final FunctionApplicationFactory factory;
        protected final MathType range;

        protected MathType domain;
        protected boolean elementsRestrict = false; //mostly false.

        public MathTypeFuncBuilder(@NotNull TypeGraph g, MathType range) {
            this(g, DEFAULT_FACTORY, range);
        }

        public MathTypeFuncBuilder(@NotNull TypeGraph g,
                @NotNull FunctionApplicationFactory factory, MathType range) {
            this.typeGraph = g;
            this.factory = factory;
            this.range = range;
        }

        public MathTypeFuncBuilder elementsRestrict(boolean e) {
            this.elementsRestrict = e;
            return this;
        }

        public MathTypeFuncBuilder paramTypes(MathType... t) {
            return paramTypes(Arrays.asList(t));
        }

        public MathTypeFuncBuilder paramTypes(List<MathType> types) {
            paramTypes.addAll(types);
            return this;
        }

        public MathTypeFuncBuilder paramNames(String... names) {
            return paramNames(Arrays.asList(names));
        }

        public MathTypeFuncBuilder paramNames(List<String> names) {
            paramNames.addAll(names);
            return this;
        }

        @Override
        public MathTypeFunc build() {
            domain = buildParameterType(typeGraph, paramNames, paramTypes);
            return new MathTypeFunc(this);
        }
    }

}
