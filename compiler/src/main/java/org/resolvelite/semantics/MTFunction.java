package org.resolvelite.semantics;

import org.resolvelite.misc.Utils.Builder;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.*;

public class MTFunction extends MTType {

    private static final FunctionApplicationFactory DEFAULT_FACTORY =
            new VanillaFunctionApplicationFactory();

    /**
     * In cases where myDomain is an instance of MTCartesian, the names of
     * the original parameters are stored in the tags of that cartesian product.
     * However, when myDomain is another type, we represent a function with
     * a SINGLE PARAMETER and we have no way to embed the name of our parameter.
     * In the latter case, this field will reflect the parameter name (or be
     * null if we represent a function with un-named parameters). In the former
     * case, the value of this field is undefined.
     */
    private final String singleParameterName;

    private final MTType domain, range;
    private final boolean restrictionFlag;
    private final FunctionApplicationFactory functionApplicationFactory;

    private final List<MTType> components = new ArrayList<>();

    private MTFunction(MTFunctionBuilder builder) {
        super(builder.g);
        if ( builder.paramNames.size() == 1 ) {
            this.singleParameterName = builder.paramNames.get(0);
        }
        else {
            this.singleParameterName = null;
        }
        this.range = builder.range;
        this.domain =
                buildParameterType(getTypeGraph(),
                        buildNullNameListOfEqualLength(builder.paramTypes),
                        builder.paramTypes);

        this.components.add(domain);
        this.components.add(range);
        this.restrictionFlag = builder.restricts;
        this.functionApplicationFactory = builder.factory;
    }

    public static MTType
            buildParameterType(TypeGraph g, List<MTType> paramTypes) {
        return buildParameterType(g,
                buildNullNameListOfEqualLength(paramTypes), paramTypes);
    }

    private static List<String> buildNullNameListOfEqualLength(
            List<MTType> original) {
        List<String> names = new ArrayList<>();
        for (MTType t : original) {
            names.add(null);
        }
        return names;
    }

    public static MTType buildParameterType(TypeGraph g,
            List<String> paramNames, List<MTType> paramTypes) {

        MTType result;
        switch (paramTypes.size()) {
        case 0:
            result = g.VOID;
            break;
        case 1:
            result = paramTypes.get(0);
            break;
        default:
            List<MTCartesian.Element> elements = new ArrayList<>();
            Iterator<String> namesIter = paramNames.iterator();
            Iterator<MTType> typesIter = paramTypes.iterator();
            while (namesIter.hasNext()) {
                elements.add(new MTCartesian.Element(namesIter.next(),
                        typesIter.next()));
            }
            result = new MTCartesian(g, elements);
        }
        return result;
    }

    public boolean parameterTypesMatch(MTFunction other,
            Comparator<MTType> comparison) {

        MTType otherDomain = other.getDomain();
        boolean result;

        if ( domain instanceof MTCartesian ) {
            result = otherDomain instanceof MTCartesian;

            if ( result ) {
                MTCartesian domainAsMTCartesian = (MTCartesian) domain;
                MTCartesian otherDomainAsMTCartesian =
                        (MTCartesian) otherDomain;

                int domainSize = domainAsMTCartesian.size();
                int otherDomainSize = otherDomainAsMTCartesian.size();

                result = (domainSize == otherDomainSize);

                if ( result ) {
                    int i = 0;
                    while (result && i < domainSize) {
                        result =
                                (comparison.compare(
                                        domainAsMTCartesian.getFactor(i),
                                        otherDomainAsMTCartesian.getFactor(i)) == 0);

                        i++;
                    }
                }
            }
        }
        else {
            result = (comparison.compare(this.domain, otherDomain) == 0);
        }
        return result;
    }

    public MTType getDomain() {
        return domain;
    }

    public MTType getRange() {
        return range;
    }

    public boolean applicationResultsKnownToContainOnlyRestrictions() {
        return restrictionFlag;
    }

    public MTType
            getApplicationType(String calledAsName, List<MTType> arguments) {
        return functionApplicationFactory.buildFunctionApplication(
                getTypeGraph(), this, calledAsName, arguments);
    }

    @Override public List<? extends MTType> getComponentTypes() {
        return components;
    }

    @Override public String toString() {
        return "(" + domain.toString() + " -> " + range.toString() + ")";
    }

    public static class MTFunctionBuilder implements Builder<MTFunction> {
        protected final TypeGraph g;
        protected final MTType range;
        protected boolean restricts;
        protected MTType domain;

        protected final List<String> paramNames = new ArrayList<>();
        protected final List<MTType> paramTypes = new ArrayList<>();
        protected final FunctionApplicationFactory factory;

        public MTFunctionBuilder(TypeGraph g,
                FunctionApplicationFactory factory, MTType range) {
            this.g = g;
            this.factory = factory;
            this.range = range;
        }

        public MTFunctionBuilder(TypeGraph g, MTType range) {
            this(g, new VanillaFunctionApplicationFactory(), range);
        }

        public MTFunctionBuilder elementsRestrict(boolean e) {
            this.restricts = e;
            return this;
        }

        public MTFunctionBuilder domain(MTType e) {
            this.domain = e;
            return this;
        }

        public MTFunctionBuilder paramNames(List<String> names) {
            this.paramNames.addAll(names);
            return this;
        }

        public MTFunctionBuilder paramNames(String... names) {
            return paramNames(Arrays.asList(names));
        }

        public MTFunctionBuilder paramTypes(List<MTType> types) {
            this.paramTypes.addAll(types);
            return this;
        }

        public MTFunctionBuilder paramTypes(MTType... types) {
            return paramTypes(Arrays.asList(types));
        }

        @Override public MTFunction build() {
            return new MTFunction(this);
        }
    }

    private static class VanillaFunctionApplicationFactory
            implements
                FunctionApplicationFactory {

        @Override public MTType buildFunctionApplication(TypeGraph g,
                MTFunction f, String calledAsName, List<MTType> arguments) {
            return new MTFunctionApplication(g, f, calledAsName, arguments);
        }
    }
}
