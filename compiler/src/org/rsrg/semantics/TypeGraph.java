package org.rsrg.semantics;

import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PApply.DisplayStyle;
import edu.clemson.resolve.proving.absyn.PApply.PApplyBuilder;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.proving.absyn.PSymbol.PSymbolBuilder;
import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.programtype.PTType;

import java.util.*;

import static org.rsrg.semantics.MTFunction.*;

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
    public final MTProper SSTR = new MTProper(this, SSET, true, "SStr");
    public final MTProper EMPTY_STRING = new MTProper(this, SSET, true,
            "Empty_String");

    public final MTProper VOID = new MTProper(this, SSET, false, "Void");

    public final MTProper BOOLEAN = new MTProper(this, SSET, false, "B");
    public final MTProper Z = new MTProper(this, SSET, true, "Z");
    public final MTProper NAT = new MTProper(this, SSET, true, "N");

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
    public final MTFunction FUNCTION = new MTFunctionBuilder(this,
            FUNCTION_CONSTRUCTOR_APPLICATION, SSET).paramTypes(SSET, SSET)
            .build();
    public final MTFunction CROSS =
            new MTFunctionBuilder(this,
                    CARTESIAN_PRODUCT_APPLICATION, MTYPE)
                    .paramTypes(MTYPE, MTYPE).build();

    public final MTFunction STR =
            new MTFunctionBuilder(this, SSTR)
                    .paramTypes(SSTR).build();

    public final MTFunction STR_CAT =
            new MTFunctionBuilder(this, SSTR)
                    .paramTypes(SSTR, SSTR).build();
    
    public final MTFunction POWERSET =
            new MTFunctionBuilder(this, POWERSET_APPLICATION, SSET)
                    .paramTypes(SSET)
                    .elementsRestrict(true).build();

    public final MTFunction RELATIONAL_FUNCTION =
            new MTFunctionBuilder(this, BOOLEAN)
                    .paramTypes(ENTITY, ENTITY)
                    .build();

    /** A function where the everything is boolean: B * B -> B */
    public final MTFunction BOOLEAN_FUNCTION =
            new MTFunctionBuilder(this, BOOLEAN)
                    .paramTypes(BOOLEAN, BOOLEAN)
                    .build();

    public final MTFunction UNION = new MTFunctionBuilder(this,
            UNION_APPLICATION, SSET).paramTypes(SSET, SSET) //
            .build();

    public final MTFunction INTERSECT = new MTFunctionBuilder(this,
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
            return new MTFunctionBuilder(g, arguments.get(1))
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

    @NotNull public PApply formConjunct(@NotNull PExp left, @NotNull PExp right) {
        PExp functionPortion = new PSymbolBuilder("and")
                .mathType(BOOLEAN_FUNCTION).build();
        return new PApplyBuilder(functionPortion).applicationType(BOOLEAN)
                .style(DisplayStyle.INFIX)
                .arguments(left, right)
                .build();
    }

    @NotNull public PApply formDisjunct(PExp left, PExp right) {
        PExp functionPortion = new PSymbolBuilder("or")
                .mathType(BOOLEAN_FUNCTION).build();
        return new PApplyBuilder(functionPortion).applicationType(BOOLEAN)
                .style(DisplayStyle.INFIX)
                .arguments(left, right)
                .build();
    }

    public final PSymbol getTrueExp() {
        return new PSymbolBuilder("true").mathType(BOOLEAN).literal(true)
                .build();
    }

    public final PSymbol getFalseExp() {
        return new PSymbolBuilder("false").mathType(BOOLEAN).literal(true)
                .build();
    }

    public final PApply formEquals(PExp left, PExp right) {
        PExp functionPortion = new PSymbolBuilder("=")
                .mathType(BOOLEAN_FUNCTION).build();
        return new PApplyBuilder(functionPortion).applicationType(BOOLEAN)
                .style(DisplayStyle.INFIX)
                .arguments(left, right)
                .build();
    }

    public final PApply formImplies(PExp left, PExp right) {
        PExp functionPortion = new PSymbolBuilder("implies")
                .mathType(RELATIONAL_FUNCTION).build();
        return new PApplyBuilder(functionPortion).applicationType(BOOLEAN)
                .style(DisplayStyle.INFIX)
                .arguments(left, right)
                .build();
    }

    public final PSymbol formConcExp() {
        return new PSymbolBuilder("conc").mathType(BOOLEAN).build();
    }

    public final PApply formInitializationPredicate(@NotNull PTType argType,
                                                    @NotNull String argName) {
        PSymbol predicateArg = new PSymbolBuilder(argName)
                .mathType(argType.toMath()).build();
        MTFunction initType = new MTFunctionBuilder(this, BOOLEAN)
                .paramTypes(argType.toMath()).build();
        PSymbol namePortion = new PSymbolBuilder(argType + ".Is_Initial")
                .mathType(initType).build();
        return new PApplyBuilder(namePortion).arguments(predicateArg)
                .applicationType(BOOLEAN).build();
    }
}
