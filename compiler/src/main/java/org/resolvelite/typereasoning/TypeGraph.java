package org.resolvelite.typereasoning;

import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.proving.absyn.PSymbol;
import org.resolvelite.proving.absyn.PSymbol.DisplayStyle;
import org.resolvelite.proving.absyn.PSymbol.PSymbolBuilder;
import org.resolvelite.semantics.*;

import java.util.*;

public class TypeGraph {

    /**
     * A set of non-thread-safe resources to be used during general type
     * reasoning. This really doesn't belong here, but anything that's reasoning
     * about types should already have access to a type graph, and only one type
     * graph is created per thread, so this is a convenient place to put it.
     */
    public final PerThreadReasoningResources threadResources =
            new PerThreadReasoningResources();
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
    public final MTProper Z = new MTProper(this, SSET, false, "Z");
    public final MTProper N = new MTProper(this, SSET, false, "N");

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
    public final MTFunction FUNCTION = new MTFunction.MTFunctionBuilder(this,
            FUNCTION_CONSTRUCTOR_APPLICATION, SSET).paramTypes(SSET, SSET)
            .build();

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

    public TypeGraph() {
        this.typeNodes = new HashMap<>();
    }

    public boolean isKnownToBeIn(MTType value, MTType expected) {
        boolean result;

        result =
                (value != MTYPE) && (value != ENTITY)
                        && isSubtype(value.getType(), expected);
        return result;
    }

    public boolean isKnownToBeIn(PExp value, MTType expected) {
        boolean result;
        try {
            PExp conditions = getValidTypeConditions(value, expected);
            result = conditions.isLiteralTrue();
        }
        catch (TypeMismatchException e) {
            result = false;
        }
        return result;
    }

    public PExp getValidTypeConditions(PExp value, MTType expected)
            throws TypeMismatchException {
        PExp result;
        MTType valueTypeValue = value.getMathTypeValue();

        if ( expected == ENTITY && valueTypeValue != MTYPE
                && valueTypeValue != ENTITY ) {
            result = getTrueExp();
        }
        else if ( valueTypeValue == MTYPE || valueTypeValue == ENTITY ) {
            //Cls and Entity aren't in anything (except hyper-whatever, which we aren't concerned with)
            throw TypeMismatchException.INSTANCE;
        }
        else if ( valueTypeValue == null ) {
            result =
                    getValidTypeConditions(value, value.getMathType(),
                            expected, EXP_VALUE_PATH);
        }
        else {
            result = getValidTypeConditions(valueTypeValue, expected);
        }
        return result;
    }

    private <V> PExp getValidTypeConditions(V foundValue, MTType foundType,
            MTType expected, NodePairPathStrategy<V> pathStrategy)
            throws TypeMismatchException {

        if ( foundType == null ) {
            throw new IllegalArgumentException(foundValue + " has no type");
        }

        Map<MTType, Map<String, MTType>> potentialFoundNodes =
                getSyntacticSubtypesWithRelationships(foundType);
        Map<MTType, Map<String, MTType>> potentialExpectedNodes =
                getSyntacticSubtypesWithRelationships(expected);

        PExp result = getFalseExp();
        PExp newCondition;

        Iterator<Map.Entry<MTType, Map<String, MTType>>> expectedEntries;
        Iterator<Map.Entry<MTType, Map<String, MTType>>> foundEntries =
                potentialFoundNodes.entrySet().iterator();
        Map.Entry<MTType, Map<String, MTType>> foundEntry, expectedEntry;

        boolean foundPath = false;

        //If foundType equals expected, we're done
        boolean foundTrivialPath = foundType.equals(expected);

        while (!foundTrivialPath && foundEntries.hasNext()) {
            foundEntry = foundEntries.next();

            expectedEntries = potentialExpectedNodes.entrySet().iterator();

            while (!foundTrivialPath && expectedEntries.hasNext()) {
                expectedEntry = expectedEntries.next();
                try {
                    newCondition =
                            getPathConditions(foundValue, foundEntry,
                                    expectedEntry, pathStrategy);

                    foundPath = foundPath | !newCondition.isLiteralFalse();
                    foundTrivialPath = newCondition.isLiteralTrue();
                    result = formDisjunct(newCondition, result);
                }
                catch (TypeMismatchException e) {}
            }
        }

        if ( foundTrivialPath ) {
            result = getTrueExp();
        }
        else if ( !foundPath ) {
            throw TypeMismatchException.INSTANCE;
        }

        return result;
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

    public void addRelationship(PExp bindingExpression, MTType destination,
            Scope environment) {

        if ( destination == null ) {
            throw new IllegalArgumentException("destination type==null");
        }

        MTType source = bindingExpression.getMathType();
        if ( source == null ) {
            throw new IllegalArgumentException("bindingExpression type==null");
        }

        CanonicalizationResult sourceCanonicalResult =
                canonicalize(source, environment, "s");
        CanonicalizationResult destinationCanonicalResult =
                canonicalize(destination, environment, "d");

        TypeRelationship relationship =
                new TypeRelationship(this,
                        destinationCanonicalResult.canonicalType,
                        bindingExpression);

        TypeNode sourceNode = getTypeNode(sourceCanonicalResult.canonicalType);
        sourceNode.addRelationship(relationship);

        //We'd like to force the presence of the destination node
        getTypeNode(destinationCanonicalResult.canonicalType);
    }

    private TypeNode getTypeNode(MTType t) {
        TypeNode result = typeNodes.get(t);

        if ( result == null ) {
            result = new TypeNode(this, t);
            typeNodes.put(t, result);
        }
        return result;
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
        if ( !result.isLiteralTrue() ) {
            //throw new UnsupportedOperationException("Cannot statically "
            //        + "establish math subtype.");
            result = getTrueExp();
        }
        return result;

    }

    public boolean isSubtype(MTType subtype, MTType supertype) {
        boolean result;

        try {
            result =
                    supertype == ENTITY || supertype == MTYPE
                            || supertype == SSET
                            // || myEstablishedSubtypes.contains(r)
                            || subtype.equals(supertype);
            // || subtype.isSyntacticSubtypeOf(supertype);
        }
        catch (NoSuchElementException nsee) {
            //Syntactic subtype checker freaks out (rightly) if there are
            //free variables in the expression, but the next check will deal
            //correctly with them.
            result = false;
        }
        return result;
    }

    private CanonicalizationResult canonicalize(MTType t, Scope environment,
            String suffix) {
        //CanonicalizingVisitor canonicalizer =
        //        new CanonicalizingVisitor(this, environment, suffix);
        //t.accept(canonicalizer);

        //TEMP. Use the visitor when ready..
        //TODO: Learn how to canonicalize MTType's properly by reading the visitor file for it
        Map<String, MTType> quantifiedVariables = new HashMap<>();
        if ( quantifiedVariables.isEmpty() ) {
            quantifiedVariables.put("", MTYPE);
        }
        MTType finalExpression = new MTBigUnion(this, quantifiedVariables, t);
        return new CanonicalizationResult(finalExpression, new HashMap<>());
    }

    private class CanonicalizationResult {
        public final MTType canonicalType;
        public final Map<String, String> canonicalToEnvironmental;

        public CanonicalizationResult(MTType canonicalType,
                Map<String, String> canonicalToOriginal) {
            this.canonicalType = canonicalType;
            this.canonicalToEnvironmental = canonicalToOriginal;
        }
    }

    private Map<MTType, Map<String, MTType>>
            getSyntacticSubtypesWithRelationships(MTType query) {

        Map<MTType, Map<String, MTType>> result = new HashMap<>();
        Map<String, MTType> bindings;

        for (MTType potential : typeNodes.keySet()) {
            try {
                bindings = query.getSyntacticSubtypeBindings(potential);
                result.put(potential, new HashMap<String, MTType>(bindings));
            }
            catch (NoSolutionException nse) {}
        }

        return result;
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
        return new PSymbolBuilder("and").mathType(BOOLEAN).arguments(p, q)
                .style(DisplayStyle.INFIX).build();
    }

    public PSymbol formDisjunct(PExp p, PExp q) {
        return new PSymbolBuilder("or").mathType(BOOLEAN).arguments(p, q)
                .style(DisplayStyle.INFIX).build();
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
        return new PSymbolBuilder("implies").mathType(BOOLEAN).arguments(p, q)
                .style(DisplayStyle.INFIX).build();
    }

    public final PSymbol formConcMetaSegment() {
        return new PSymbolBuilder("conc").mathType(MTYPE).build();
    }
}
