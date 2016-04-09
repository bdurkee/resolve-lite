package org.rsrg.semantics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.MathCartesianClassification.Element;

import java.util.*;
import java.util.stream.Collectors;

public class MathFunctionClassification extends MathClassification {

    private static final FunctionApplicationFactory DEFAULT_FACTORY =
            new VanillaFunctionApplicationFactory();

    private final List<MathClassification> paramTypes = new LinkedList<>();
    private final List<String> paramNames = new LinkedList<>();

    private MathClassification domainType, resultType;

    private final FunctionApplicationFactory applicationFactory;

    public MathFunctionClassification(@NotNull DumbTypeGraph g,
                                      @NotNull MathClassification range,
                                      @NotNull MathClassification... paramTypes) {
        this(g, range, Arrays.asList(paramTypes));
    }

    public MathFunctionClassification(@NotNull DumbTypeGraph g,
                                      @NotNull MathClassification range,
                                      @NotNull List<MathClassification> paramTypes) {
        this(g, DEFAULT_FACTORY, range, paramTypes);
    }

    public MathFunctionClassification(@NotNull DumbTypeGraph g,
                                      @NotNull MathClassification range,
                                      @NotNull List<String> paramNames,
                                      @NotNull List<MathClassification> paramTypes) {
        this(g, DEFAULT_FACTORY, range, paramNames, paramTypes);
    }


    public MathFunctionClassification(@NotNull DumbTypeGraph g,
                                      @Nullable FunctionApplicationFactory apply,
                                      @NotNull MathClassification range,
                                      @NotNull MathClassification... paramTypes) {
        this(g, apply, range, Arrays.asList(paramTypes));
    }

    public MathFunctionClassification(@NotNull DumbTypeGraph g,
                                      @Nullable FunctionApplicationFactory apply,
                                      @NotNull MathClassification range,
                                      @NotNull List<MathClassification> paramTypes) {
        this(g, apply, range, buildDummyNameListOfEqualLength(paramTypes),
                paramTypes);
    }

    private MathFunctionClassification(@NotNull DumbTypeGraph g,
                                       @Nullable FunctionApplicationFactory applyFactory,
                                       @NotNull MathClassification range,
                                       @NotNull List<String> paramNames,
                                       @NotNull List<MathClassification> paramTypes) {
        super(g, range);
        this.paramTypes.addAll(expandAsNeeded(paramTypes));
        this.resultType = range;
        this.paramNames.addAll(paramNames);
        this.domainType = buildDomainType();

        if (applyFactory == null) {
            this.applicationFactory = DEFAULT_FACTORY;
        }
        else {
            this.applicationFactory = applyFactory;
        }
        this.typeRefDepth = range.typeRefDepth;
    }

    private static List<MathClassification> expandAsNeeded(
            @NotNull List<MathClassification> t) {
        List<MathClassification> result = new ArrayList<>();
        for (MathClassification c : t) {
            result.addAll(expandAsNeeded(c));
        }
        return result;
    }

    private static List<MathClassification> expandAsNeeded(
            @NotNull MathClassification t) {
        List<MathClassification> result = new ArrayList<>();

        if (t instanceof MathCartesianClassification) {
            MathCartesianClassification domainAsMTCartesian =
                    (MathCartesianClassification) t;

            int size = domainAsMTCartesian.size();
            for (int i = 0; i < size; i++) {
                result.add(domainAsMTCartesian.getFactor(i));
            }
        }
        else {
            if (!t.equals(t.getTypeGraph().VOID)) {
                result.add(t);
            }
        }
        return result;
    }

    @Override public MathClassification getEnclosingClassification() {
        return this;
    }

    private static List<String> buildDummyNameListOfEqualLength(
            List<MathClassification> original) {
        return original.stream().map(t -> "").collect(Collectors.toList());
    }

    private MathClassification buildDomainType() {
        if (paramTypes.isEmpty()) {
            domainType = g.VOID;
        }
        else if (paramTypes.size() == 1) {
            domainType = paramTypes.get(0);
        }
        else {
            List<Element> elements = new ArrayList<>();
            Iterator<String> namesIter = paramNames.iterator();
            Iterator<MathClassification> typesIter = paramTypes.iterator();
            while (namesIter.hasNext()) {
                elements.add(new Element(namesIter.next(), typesIter.next()));
            }
            domainType = new MathCartesianClassification(g, elements);
        }
        return domainType;
    }

    @Override public List<MathClassification> getComponentTypes() {
        List<MathClassification> result = new ArrayList<>();
        result.add(domainType);
        result.add(resultType);
        return result;
    }

    @Override public MathClassification withVariablesSubstituted(
            Map<MathClassification, MathClassification> substitutions) {
        return new MathFunctionClassification(g, applicationFactory,
                resultType.withVariablesSubstituted(substitutions),
                domainType.withVariablesSubstituted(substitutions));
    }

    public MathClassification deschematize(@NotNull List<MathClassification> argTypes)
            throws BindingException {
        //for each supplied actual type
        Map<MathClassification, MathClassification> bindingsSoFar = new HashMap<>();
        Iterator<MathClassification> argTypeIter = argTypes.iterator();

        for (MathClassification formalParameterType : paramTypes) {
            formalParameterType =
                    formalParameterType
                            .withVariablesSubstituted(bindingsSoFar);

            //We know arguments and formalParameterTypes are the same
            //length, see above
            MathClassification argumentType = argTypeIter.next();

            if (formalParameterType.containsSchematicType()) {

                Map<MathClassification, MathClassification> iterationBindings = new HashMap<>();
                bind(argumentType, formalParameterType, iterationBindings);
                bindingsSoFar.putAll(iterationBindings);
            }
        }
        MathClassification thisDeschematized =
                this.withVariablesSubstituted(bindingsSoFar);
        return thisDeschematized;
    }

    //TODO : Should throw binding exception
    //think about it in terms of formal, t2: (D : SSet) -> D and
    // actual, t1:  N -> N
    //here, N is the concrete type and 'D' is the template type. Note that now
    //we no longer need a map from the name of the schematic type (in this 'D')
    //to SSet -- as this is now encoded in {@code t2}.
    public void bind(@NotNull MathClassification t1, @NotNull MathClassification t2,
                     @NotNull Map<MathClassification, MathClassification> bindingsAccumulator)
            throws BindingException {

        if (t2.identifiesSchematicType) {
            //attempt to bind concrete t1 to template type t2
            if (g.isSubtype(t1, t2.getEnclosingClassification())) {
                if (t2 instanceof MathNamedClassification &&
                        !bindingsAccumulator.containsKey(t1)) {
                    bindingsAccumulator.put(t2, t1);
                }
            }
        }
        List<MathClassification> t1Components = t1.getComponentTypes();
        List<MathClassification> t2Components = t2.getComponentTypes();
        if ( t1Components.size() != t2Components.size() ) throw new BindingException(t1, t2);

        Iterator<MathClassification> t1Iter = t1Components.iterator();
        Iterator<MathClassification> t2Iter = t2Components.iterator();
        while ( t1Iter.hasNext() ) {
            bind(t1Iter.next(), t2Iter.next(), bindingsAccumulator);
        }
    }

    //really only care about the type ref depth of the result.
    public int getTypeRefDepth() {
        return resultType.getTypeRefDepth();
    }

    public List<MathClassification> getParamTypes() {
        return paramTypes;
    }

    public MathClassification getDomainType() {
        return domainType;
    }

    public MathClassification getResultType() {
        return resultType;
    }

    public MathClassification getApplicationType(@NotNull String calledAsName,
                                                 @NotNull MathClassification... args) {
        return getApplicationType(calledAsName, Arrays.asList(args));
    }

    public MathClassification getApplicationType(@NotNull String calledAsName,
                                                 @NotNull List<MathClassification> arguments) {
        return applicationFactory.buildFunctionApplication(
                g, this, calledAsName, arguments);
    }

    @Override public String toString() {
        return "("+domainType+" ⟶ "+resultType+")";
    }

    private static class VanillaFunctionApplicationFactory
            implements
            FunctionApplicationFactory {

        @Override public MathClassification buildFunctionApplication(
                @NotNull DumbTypeGraph g, @NotNull MathFunctionClassification f,
                @NotNull String calledAsName,
                @NotNull List<MathClassification> arguments) {
            return new MathFunctionApplicationClassification(g, f, calledAsName, arguments);
        }
    }
}
