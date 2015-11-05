package org.rsrg.semantics;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.proving.absyn.PSymbol.PSymbolBuilder;
import org.rsrg.semantics.programtype.PTType;

import java.util.*;

public class TypeGraph {

    private final ExpValuePathStrategy EXP_VALUE_PATH =
            new ExpValuePathStrategy();
    private final MTTypeValuePathStrategy MTTYPE_VALUE_PATH =
            new MTTypeValuePathStrategy();

    private final HashMap<MTType, TypeNode> typeNodes;

    public final MTInvalid INVALID = MTInvalid.getInstance(this);
    public final MTType ELEMENT = new MTProper(this, "Element");
    public final MTProper ENTITY = new MTProper(this, "Entity");

    public final MTProper MTYPE = new MTProper(this, null, true, "MType");
    public final MTProper SSET = new MTProper(this, MTYPE, true, "SSet");
    public final MTProper VOID = new MTProper(this, SSET, false, "Void");
    public final MTProper CARD = new MTProper(this, MTYPE, false, "Card");

    public final MTProper BOOLEAN = new MTProper(this, SSET, false, "B");

    public final MTProper BASE_POINT = new MTProper(this, SSET, false,
            "Base_Point");
    public final MTProper EMPTY_SET = new MTProper(this, SSET, false,
            "Empty_Set");

    private final static FunctionApplicationFactory POWERSET_APPLICATION =
            new PowersetApplicationFactory();
    private final static FunctionApplicationFactory UNION_APPLICATION =
            new UnionApplicationFactory();
    private final static FunctionApplicationFactory INTERSECT_APPLICATION =
            new IntersectApplicationFactory();
    private final static FunctionApplicationFactory FUNCTION_CONSTRUCTOR_APPLICATION =
            new FunctionConstructorApplicationFactory();
    private final static FunctionApplicationFactory CARTESIAN_PRODUCT_APPLICATION =
            new CartesianProductApplicationFactory();
    public final MTFunction FUNCTION = new MTFunction.MTFunctionBuilder(this,
            FUNCTION_CONSTRUCTOR_APPLICATION, SSET).paramTypes(SSET, SSET)
            .build();
    public final MTFunction CROSS =
            new MTFunction.MTFunctionBuilder(this,
                    CARTESIAN_PRODUCT_APPLICATION, MTYPE)
                    .paramTypes(MTYPE, MTYPE).build();

    public final MTFunction POWERSET = //
            new MTFunction.MTFunctionBuilder(this, POWERSET_APPLICATION, SSET) //
                    .paramTypes(SSET) //
                    .elementsRestrict(true).build();

    public final MTFunction UNION = new MTFunction.MTFunctionBuilder(this,
            UNION_APPLICATION, SSET).paramTypes(SSET, SSET) //
            .build();

    public final MTFunction INTERSECT = new MTFunction.MTFunctionBuilder(this,
            INTERSECT_APPLICATION, SSET).paramTypes(SSET, SSET) //
            .build();

    private static class PowersetApplicationFactory
            implements
                FunctionApplicationFactory {

        @Override public MTType buildFunctionApplication(TypeGraph g,
                MTFunction f, String refName, List<MTType> args) {
            return new MTPowersetApplication(g, args.get(0));
        }
    }

    private static class UnionApplicationFactory
            implements
                FunctionApplicationFactory {

        @Override public MTType buildFunctionApplication(TypeGraph g,
                MTFunction f, String calledAsName, List<MTType> arguments) {
            return new MTUnion(g, arguments);
        }
    }

    private static class IntersectApplicationFactory
            implements
                FunctionApplicationFactory {

        @Override public MTType buildFunctionApplication(TypeGraph g,
                MTFunction f, String calledAsName, List<MTType> arguments) {
            return new MTUnion(g, arguments);
        }
    }

    private static class FunctionConstructorApplicationFactory
            implements
                FunctionApplicationFactory {

        @Override public MTType buildFunctionApplication(TypeGraph g,
                MTFunction f, String calledAsName, List<MTType> arguments) {
            return new MTFunction.MTFunctionBuilder(g, arguments.get(1))
                    .paramTypes(arguments.get(0)).build();
        }
    }

    private static class CartesianProductApplicationFactory
            implements
            FunctionApplicationFactory {

        @Override
        public MTType buildFunctionApplication(TypeGraph g, MTFunction f,
                               String calledAsName, List<MTType> arguments) {
            return new MTCartesian(g,
                    new MTCartesian.Element(arguments.get(0)),
                    new MTCartesian.Element(arguments.get(1)));
        }
    }

    public TypeGraph() {
        this.typeNodes = new HashMap<>();
    }

    public boolean isKnownToBeIn(MTType value, MTType expected) {
        boolean result;

        result =
                (value != MTYPE) && (value != ENTITY)
                        && isSubtype(value.getType(), expected)
                        || value.equals(expected);
        return result;
    }

    public boolean isKnownToBeIn(PExp value, MTType expected) {
        boolean result;
        try {
            PExp conditions = getValidTypeConditions(value, expected);
            result = conditions.isObviouslyTrue();
        }
        catch (TypeMismatchException e) {
            result = false;
        }
        return result;
    }

    public PExp getValidTypeConditions(PExp value, MTType expected)
            throws TypeMismatchException {
        return getTrueExp();
    }

    private <V> PExp getValidTypeConditions(V foundValue, MTType foundType,
            MTType expected, NodePairPathStrategy<V> pathStrategy)
            throws TypeMismatchException {
        return getTrueExp();
    }

    /**
     * Returns the conditions required to establish that {@code foundValue} is a
     * member of the type represented by {@code expectedEntry} along
     * the path from {@code foundEntry} to {@code expectedEntry}. If
     * no such conditions exist (i.e., if the conditions would be false),
     * throws a {@code TypeMismatchException}.
     * 
     * @param foundValue The value we'd like to establish is in the type
     *        represented by {@code expectedEntry}.
     * @param foundEntry A node in the type graph of which {@code foundValue} is
     *        a syntactic subtype.
     * @param expectedEntry A node in the type graph of which representing a
     *        type in which we would like to establish {@code foundValue}
     *        resides.
     * @param pathStrategy The strategy for following the path between
     *        {@code foundEntry} and {@code expectedEntry}.
     * 
     * @return The conditions under which the path can be followed.
     * 
     * @throws TypeMismatchException If the conditions under which the path can
     *         be followed would be false.
     */
    private <V> PExp getPathConditions(V foundValue,
            Map.Entry<MTType, Map<String, MTType>> foundEntry,
            Map.Entry<MTType, Map<String, MTType>> expectedEntry,
            NodePairPathStrategy<V> pathStrategy) throws TypeMismatchException {

        Map<String, MTType> combinedBindings = new HashMap<String, MTType>();

        combinedBindings.clear();
        combinedBindings.putAll(updateMapLabels(foundEntry.getValue(), "_s"));
        combinedBindings
                .putAll(updateMapLabels(expectedEntry.getValue(), "_d"));

        PExp newCondition =
                pathStrategy.getValidTypeConditionsBetween(foundValue,
                        foundEntry.getKey(), expectedEntry.getKey(),
                        combinedBindings);

        return newCondition;
    }

    private PExp getValidTypeConditions(MTType value, MTType expected)
            throws TypeMismatchException {
        PExp result = getFalseExp();

        if ( expected == SSET ) {
            //Every MTType is in SSet except for Entity and Cls
            result = getTrueExp();
        }

        /*else if (expected instanceof MTPowertypeApplication) {
            if (value.equals(EMPTY_SET)) {
                //The empty set is in all powertypes
                result = getTrueVarExp();
            }
            else {
                //If "expected" happens to be Power(t) for some t, we can
                //"demote" value to an INSTANCE of itself (provided it is not
                //the empty set), and expected to just t
                MTPowertypeApplication expectedAsPowertypeApplication =
                        (MTPowertypeApplication) expected;

                DummyExp memberOfValue = new DummyExp(value);

                if (isKnownToBeIn(memberOfValue, expectedAsPowertypeApplication
                        .getArgument(0))) {

                    result = getTrueVarExp();
                }
            }
        }*/

        //If we've already established it statically, no need for further work
        if ( !result.isObviouslyTrue() ) {
            //throw new UnsupportedOperationException("Cannot statically "
            //        + "establish math subtype.");
            result = getTrueExp();
        }
        return result;

    }

    public boolean isSubtype(MTType subtype, MTType supertype) {
        return true;
    }

    public static MTType getCopyWithVariablesSubstituted(MTType original,
            Map<String, MTType> substitutions) {
        VariableReplacingVisitor renamer =
                new VariableReplacingVisitor(substitutions);
        original.accept(renamer);
        return renamer.getFinalExpression();
    }

    private interface NodePairPathStrategy<V> {

        public PExp getValidTypeConditionsBetween(V sourceValue,
                                                  MTType sourceType, MTType expectedType,
                                                  Map<String, MTType> bindings) throws TypeMismatchException;
    }

    private class ExpValuePathStrategy implements NodePairPathStrategy<PExp> {

        @Override public PExp getValidTypeConditionsBetween(PExp sourceValue,
                MTType sourceType, MTType expectedType,
                Map<String, MTType> bindings) throws TypeMismatchException {

            return typeNodes.get(sourceType).getValidTypeConditionsTo(
                    sourceValue, expectedType, bindings);
        }
    }

    private class MTTypeValuePathStrategy
            implements
                NodePairPathStrategy<MTType> {

        @Override public PExp getValidTypeConditionsBetween(MTType sourceValue,
                MTType sourceType, MTType expectedType,
                Map<String, MTType> bindings) throws TypeMismatchException {

            return typeNodes.get(sourceType).getValidTypeConditionsTo(
                    sourceValue, expectedType, bindings);
        }
    }

    private <T> Map<String, T> updateMapLabels(Map<String, T> original,
            String suffix) {

        Map<String, T> result = new HashMap<String, T>();
        for (Map.Entry<String, T> entry : original.entrySet()) {
            result.put(entry.getKey() + suffix, entry.getValue());
        }

        return result;
    }

    public PExp formConjuncts(PExp... e) {
        return formConjuncts(Arrays.asList(e));
    }

    public PExp formConjuncts(List<PExp> e) {
        if ( e == null ) {
            throw new IllegalArgumentException("can't conjunct a null list");
        }
        if ( e.isEmpty() ) return null;
        Iterator<PExp> segsIter = e.iterator();
        PExp result = segsIter.next();
        if ( e.size() == 1 ) {
            return e.get(0);
        }
        while (segsIter.hasNext()) {
            result = formConjunct(result, segsIter.next());
        }
        return result;
    }

    public PSymbol formConjunct(PExp p, PExp q) {
        return null;
        //return new PSymbolBuilder("and").mathType(BOOLEAN).arguments(p, q)
        //        .style(DisplayStyle.INFIX).build();
    }

    public PSymbol formDisjunct(PExp p, PExp q) {
        return null;
        //return new PSymbolBuilder("or").mathType(BOOLEAN).arguments(p, q)
        //        .style(DisplayStyle.INFIX).build();
    }

    public final PSymbol getTrueExp() {
        return new PSymbolBuilder("true").mathType(BOOLEAN).literal(true)
                .build();
    }

    public final PSymbol getFalseExp() {
        return new PSymbolBuilder("false").mathType(BOOLEAN).literal(true)
                .build();
    }

    public final PSymbol formImplies(PExp p, PExp q) {
        return null;
        //return new PSymbolBuilder("implies").mathType(BOOLEAN).arguments(p, q)
        //        .style(DisplayStyle.INFIX).build();
    }

    public final PSymbol formConcExp() {
        return new PSymbolBuilder("conc").mathType(BOOLEAN).build();
    }

    public final PSymbol formInitializationPredicate(PTType argType,
            String argName) {
        PSymbol predicateArg =
                new PSymbolBuilder(argName).mathType(argType.toMath()).build();
        return null;
        //return new PSymbolBuilder(argType.toString() + ".Is_Initial")
        //        .mathType(BOOLEAN).arguments(predicateArg).build();
    }
}
