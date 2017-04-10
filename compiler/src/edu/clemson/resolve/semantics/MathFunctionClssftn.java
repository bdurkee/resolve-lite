package edu.clemson.resolve.semantics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import edu.clemson.resolve.semantics.MathCartesianClssftn.Element;

import java.util.*;
import java.util.stream.Collectors;

public class MathFunctionClssftn extends MathClssftn {

    private static final FunctionApplicationFactory DEFAULT_FACTORY = new VanillaFunctionApplicationFactory();
    private final List<MathClssftn> paramTypes = new LinkedList<>();
    private final List<String> paramNames = new LinkedList<>();

    private MathClssftn domainType, resultType;

    private final FunctionApplicationFactory applicationFactory;

    public MathFunctionClssftn(@NotNull DumbMathClssftnHandler g,
                               @NotNull MathClssftn range,
                               @NotNull MathClssftn... paramTypes) {
        this(g, range, Arrays.asList(paramTypes));
    }

    public MathFunctionClssftn(@NotNull DumbMathClssftnHandler g,
                               @NotNull MathClssftn range,
                               @NotNull List<MathClssftn> paramTypes) {
        this(g, DEFAULT_FACTORY, range, paramTypes);
    }

    public MathFunctionClssftn(@NotNull DumbMathClssftnHandler g,
                               @NotNull MathClssftn range,
                               @NotNull List<String> paramNames,
                               @NotNull List<MathClssftn> paramTypes) {
        this(g, DEFAULT_FACTORY, range, paramNames, paramTypes);
    }


    public MathFunctionClssftn(@NotNull DumbMathClssftnHandler g,
                               @Nullable FunctionApplicationFactory apply,
                               @NotNull MathClssftn range,
                               @NotNull MathClssftn... paramTypes) {
        this(g, apply, range, Arrays.asList(paramTypes));
    }

    public MathFunctionClssftn(@NotNull DumbMathClssftnHandler g,
                               @Nullable FunctionApplicationFactory apply,
                               @NotNull MathClssftn range,
                               @NotNull List<MathClssftn> paramTypes) {
        this(g, apply, range, buildDummyNameListOfEqualLength(paramTypes),
                paramTypes);
    }

    private MathFunctionClssftn(@NotNull DumbMathClssftnHandler g,
                                @Nullable FunctionApplicationFactory applyFactory,
                                @NotNull MathClssftn range,
                                @NotNull List<String> paramNames,
                                @NotNull List<MathClssftn> paramTypes) {
        super(g, range);
        this.paramTypes.addAll(expandAsNeeded(paramTypes));
        this.resultType = range;
        this.paramNames.addAll(paramNames);
        this.domainType = buildParameterType(g, paramNames, paramTypes);

        if (applyFactory == null) {
            this.applicationFactory = DEFAULT_FACTORY;
        }
        else {
            this.applicationFactory = applyFactory;
        }
        this.typeRefDepth = range.typeRefDepth;
    }

    public static MathClssftn buildParameterType(DumbMathClssftnHandler g,
                                                 List<String> paramNames,
                                                 List<MathClssftn> paramTypes) {

        MathClssftn result;
        switch (paramTypes.size()) {
            case 0:
                result = g.VOID;
                break;
            case 1:
                result = paramTypes.get(0);
                break;
            default:
                List<Element> elements = new LinkedList<>();

                Iterator<String> namesIter = paramNames.iterator();
                Iterator<MathClssftn> typesIter = paramTypes.iterator();
                while (namesIter.hasNext()) {
                    elements.add(new Element(namesIter.next(), typesIter.next()));
                }
                result = new MathCartesianClssftn(g, elements);
        }
        return result;
    }

    public static List<MathClssftn> expandAsNeeded(@NotNull List<MathClssftn> t) {
        List<MathClssftn> result = new ArrayList<>();
        if (t.size() == 1) {
            result.addAll(expandAsNeeded(t.get(0)));
        }
        else {
            result.addAll(t);
        }
        return result;
    }

    public static List<MathClssftn> expandAsNeeded(@NotNull MathClssftn t) {
        List<MathClssftn> result = new ArrayList<>();

        if (t instanceof MathCartesianClssftn) {
            MathCartesianClssftn domainAsMTCartesian = (MathCartesianClssftn) t;

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

    @Override
    public MathClssftn getEnclosingClassification() {
        return this;
    }

    private static List<String> buildDummyNameListOfEqualLength(List<MathClssftn> original) {
        return original.stream().map(t -> "").collect(Collectors.toList());
    }

    @Override
    public List<MathClssftn> getComponentTypes() {
        List<MathClssftn> result = new ArrayList<>();
        result.add(domainType);
        result.add(resultType);
        return result;
    }

    @Override
    public MathClssftn withVariablesSubstituted(
            Map<String, MathClssftn> substitutions) {
        return new MathFunctionClssftn(g, applicationFactory,
                resultType.withVariablesSubstituted(substitutions),
                domainType.withVariablesSubstituted(substitutions));
    }

    public MathClssftn deschematize(@NotNull List<MathClssftn> argTypes)  throws BindingException {
        //for each supplied actual type
        Map<String, MathClssftn> bindingsSoFar = new HashMap<>();
        Iterator<MathClssftn> argTypeIter = argTypes.iterator();

        for (MathClssftn formalParameterType : paramTypes) {
            formalParameterType = formalParameterType.withVariablesSubstituted(bindingsSoFar);
            //We know arguments and formalParameterTypes are the same
            //length, see above
            MathClssftn argumentType = argTypeIter.next();

            if (formalParameterType.containsSchematicType()) {
                Map<String, MathClssftn> iterationBindings = new HashMap<>();
                try {
                    bind(argumentType, formalParameterType, iterationBindings);
                } catch (BindingException be) {
                }
                bindingsSoFar.putAll(iterationBindings);
            }
        }
        MathClssftn thisDeschematized = this.withVariablesSubstituted(bindingsSoFar);
        return thisDeschematized;
    }

    //TODO : Should throw binding exception
    //think about it in terms of formal, t2: (D : SSet) -> D and
    // actual, t1:  N -> N
    //here, N is the concrete type and 'D' is the template type. Note that now
    //we no longer need a map from the name of the schematic type (in this 'D')
    //to SSet -- as this is now encoded in {@code t2}.
    public void bind(@NotNull MathClssftn t1, @NotNull MathClssftn t2,
                     @NotNull Map<String, MathClssftn> bindingsAccumulator)
            throws BindingException {

        if (t2.identifiesSchematicType) {
            //attempt to bind concrete t1 to template type t2
            if (g.isSubtype(t1, t2.getEnclosingClassification())) {
                if (t2 instanceof MathNamedClssftn &&
                        !containsBinding(t1, bindingsAccumulator)) {
                    bindingsAccumulator.put(((MathNamedClssftn) t2).tag, t1);
                }
            }
        }
        List<MathClssftn> t1Components = t1.getComponentTypes();
        List<MathClssftn> t2Components = t2.getComponentTypes();
        if (t1Components.size() != t2Components.size())
            return;//throw new BindingException(t1, t2);

        Iterator<MathClssftn> t1Iter = t1Components.iterator();
        Iterator<MathClssftn> t2Iter = t2Components.iterator();
        while (t1Iter.hasNext()) {
            bind(t1Iter.next(), t2Iter.next(), bindingsAccumulator);
        }
    }

    /**
     * Returns {@code true} if {@code currentBindings} contains e;
     * {@code false} otherwise.
     */
    private boolean containsBinding(MathClssftn e,
                                    Map<String, MathClssftn> currentBindings) {
        return e instanceof MathNamedClssftn &&
                currentBindings.containsKey(((MathNamedClssftn) e).tag);
    }

    //really only care about the type ref depth of the result.
    public int getTypeRefDepth() {
        return resultType.getTypeRefDepth();
    }

    public List<MathClssftn> getParamTypes() {
        return paramTypes;
    }

    public MathClssftn getDomainType() {
        return domainType;
    }

    public MathClssftn getRangeClssftn() {
        return resultType;
    }

    public MathClssftn getApplicationType(@NotNull String calledAsName,
                                          @NotNull MathClssftn... args) {
        return getApplicationType(calledAsName, Arrays.asList(args));
    }

    public MathClssftn getApplicationType(@NotNull String calledAsName,
                                          @NotNull List<MathClssftn> arguments) {
        return applicationFactory.buildFunctionApplication(
                g, this, calledAsName, arguments);
    }

    @Override
    public String toString() {
        return "(" + domainType + " ⟶ " + resultType + ")";
    }

    private static class VanillaFunctionApplicationFactory
            implements
            FunctionApplicationFactory {

        @Override
        public MathClssftn buildFunctionApplication(@NotNull DumbMathClssftnHandler g,
                                                    @NotNull MathFunctionClssftn f,
                                                    @NotNull String calledAsName,
                                                    @NotNull List<MathClssftn> arguments) {
            return new MathFunctionApplicationClssftn(g, f, calledAsName, arguments);
        }
    }
}
