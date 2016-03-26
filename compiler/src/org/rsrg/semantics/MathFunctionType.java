package org.rsrg.semantics;

import edu.clemson.resolve.misc.Utils.Builder;
import edu.clemson.resolve.proving.absyn.PExp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class MathFunctionType extends MathType {

    private static final FunctionApplicationFactory DEFAULT_FACTORY =
            new VanillaFunctionApplicationFactory();

    private final List<MathType> paramTypes = new LinkedList<>();
    private MathType domainType, resultType;

    private final FunctionApplicationFactory applicationFactory;
    private final Map<String, MathType> schematicTypes = new LinkedHashMap<>();

    public MathFunctionType(@NotNull DumbTypeGraph g,
                            @NotNull MathType range,
                            @NotNull MathType ... paramTypes) {
        this(g, range, Arrays.asList(paramTypes));
    }

    public MathFunctionType(@NotNull DumbTypeGraph g,
                            @NotNull MathType range,
                            @NotNull List<MathType> paramTypes) {
        this(g, DEFAULT_FACTORY, range, paramTypes);
    }

    public MathFunctionType(@NotNull DumbTypeGraph g,
                            @Nullable FunctionApplicationFactory apply,
                            @NotNull MathType range,
                            @NotNull MathType ... paramTypes) {
        this(g, apply, range, Arrays.asList(paramTypes));
    }

    public MathFunctionType(@NotNull DumbTypeGraph g,
                            @Nullable FunctionApplicationFactory apply,
                            @NotNull MathType range,
                            @NotNull List<MathType> paramTypes) {
        this(g, apply, range, buildDummyNameListOfEqualLength(paramTypes),
                paramTypes);
    }

    private MathFunctionType(@NotNull DumbTypeGraph g,
                             @Nullable FunctionApplicationFactory applyFactory,
                             @NotNull MathType range,
                             @NotNull List<String> paramNames,
                             @NotNull List<MathType> paramTypes) {
        super(g, range);
        this.paramTypes.addAll(paramTypes);
        this.resultType = range;
        this.domainType = buildDomainType();

        if (applyFactory == null) {
            this.applicationFactory = DEFAULT_FACTORY;
        }
        else {
            this.applicationFactory = applyFactory;
        }
        this.typeRefDepth = range.typeRefDepth;
    }

    @Override public MathType getEnclosingType() {
        return this;
    }

    private static List<String> buildDummyNameListOfEqualLength(
            List<MathType> original) {
        return original.stream().map(t -> "").collect(Collectors.toList());
    }

    private MathType buildDomainType() {
        if (paramTypes.isEmpty()) {
            domainType = g.VOID;
        }
        else if (paramTypes.size() == 1) {
            domainType = paramTypes.get(0);
        }
        else {
            domainType = new MathCartesianType(g, paramTypes);
        }
        return domainType;
    }

    @Override public List<MathType> getComponentTypes() {
        List<MathType> result = new ArrayList<>();
        result.add(domainType);
        result.add(resultType);
        return result;
    }

    @Override public MathType withVariablesSubstituted(
            Map<MathType, MathType> substitutions) {
        return new MathFunctionType(g, applicationFactory,
                resultType.withVariablesSubstituted(substitutions),
                domainType.withVariablesSubstituted(substitutions));
    }

    //ok, if there's an implicit type parameter what we need to do is get its enclosing type
    //and return a new version of the formal functions signature. For example:
    //if we have Foo(T : SSet; x : T) : B
    //then we will operate on Foo(
    public MathType deschematize(@NotNull List<MathType> argTypes)
            throws BindingException {
        //for each supplied actual type
        Map<MathType, MathType> bindingsSoFar = new HashMap<>();
        Iterator<MathType> argTypeIter = argTypes.iterator();

        for (MathType formalParameterType : paramTypes) {
            formalParameterType =
                    formalParameterType
                            .withVariablesSubstituted(bindingsSoFar);

            //We know arguments and formalParameterTypes are the same
            //length, see above
            MathType argumentType = argTypeIter.next();

            if (formalParameterType.containsSchematicType()) {

                Map<MathType, MathType> iterationBindings = new HashMap<>();
                bind(argumentType, formalParameterType, iterationBindings);
                bindingsSoFar.putAll(iterationBindings);
            }
        }
        MathType thisDeschematized =
                this.withVariablesSubstituted(bindingsSoFar);
        return thisDeschematized;
    }

    //TODO : Should throw binding exception
    //think about it in terms of formal, t2: (D : SSet) -> D and
    // actual, t1:  N -> N
    //here, N is the concrete type and 'D' is the template type. Note that now
    //we no longer need a map from the name of the schematic type (in this 'D')
    //to SSet -- as this is now encoded in {@code t2}.
    public void bind(@NotNull MathType t1, @NotNull MathType t2,
                     @NotNull Map<MathType, MathType> bindingsAccumulator)
            throws BindingException {

        if (t2.identifiesSchematicType) {
            //attempt to bind concrete t1 to template type t2
            if (g.isSubtype(t1, t2.getEnclosingType()) ||
                    t1.equals(t2.getEnclosingType())) {
                if (t2 instanceof MathNamedType && !bindingsAccumulator.containsKey(t1)) {

                    bindingsAccumulator.put(t2, t1);
                }
            }
        }
        List<MathType> t1Components = t1.getComponentTypes();
        List<MathType> t2Components = t2.getComponentTypes();
        if ( t1Components.size() != t2Components.size() ) throw new BindingException(t1, t2);

        Iterator<MathType> t1Iter = t1Components.iterator();
        Iterator<MathType> t2Iter = t2Components.iterator();
        while ( t1Iter.hasNext() ) {
            bind(t1Iter.next(), t2Iter.next(), bindingsAccumulator);
        }
    }

    //really only care about the type ref depth of the result.
    public int getTypeRefDepth() {
        return resultType.getTypeRefDepth();
    }

    public List<MathType> getParamTypes() {
        return paramTypes;
    }

    public MathType getDomainType() {
        return domainType;
    }

    public MathType getResultType() {
        return resultType;
    }

    public MathType getApplicationType(@NotNull String calledAsName,
                                       @NotNull MathType ... args) {
        return getApplicationType(calledAsName, Arrays.asList(args));
    }

    public MathType getApplicationType(@NotNull String calledAsName,
                                       @NotNull List<MathType> arguments) {
        return applicationFactory.buildFunctionApplication(
                g, this, calledAsName, arguments);
    }

    @Override public String toString() {
        return "("+domainType+" -> "+resultType+")";
    }

    private static class VanillaFunctionApplicationFactory
            implements
            FunctionApplicationFactory {

        @Override public MathType buildFunctionApplication(
                @NotNull DumbTypeGraph g, @NotNull MathFunctionType f,
                @NotNull String calledAsName,
                @NotNull List<MathType> arguments) {
            return new MathFunctionApplicationType(g, f, calledAsName, arguments);
        }
    }
}
