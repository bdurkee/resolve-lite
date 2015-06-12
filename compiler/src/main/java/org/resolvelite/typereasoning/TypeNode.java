package org.resolvelite.typereasoning;

import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.semantics.MTType;
import org.resolvelite.semantics.NoSolutionException;
import org.resolvelite.semantics.TypeMismatchException;

import java.util.*;

public class TypeNode {

    private static final ExpValuePathStrategy EXP_VALUE_PATH =
            new ExpValuePathStrategy();

    private static final MTTypeValuePathStrategy MTTYPE_VALUE_PATH =
            new MTTypeValuePathStrategy();

    private MTType type;
    private Map<MTType, Set<TypeRelationship>> relationships;
    private final TypeGraph typeGraph;

    public TypeNode(TypeGraph g, MTType type) {
        this.type = type;
        this.relationships = new HashMap<MTType, Set<TypeRelationship>>();
        this.typeGraph = g;
    }

    public MTType getType() {
        return type;
    }

    public int getOutgoingRelationshipCount() {
        return relationships.size();
    }

    private <V> PExp getValidTypeConditionsTo(V value, MTType dst,
            Map<String, MTType> bindings,
            RelationshipPathStrategy<V> pathStrategy)
            throws TypeMismatchException {

        if ( !relationships.containsKey(dst) ) {
            throw TypeMismatchException.INSTANCE;
        }

        PExp finalConditions = typeGraph.getFalseExp();
        Set<TypeRelationship> relationships = this.relationships.get(dst);
        boolean foundTrivialPath = false;
        Iterator<TypeRelationship> relationshipIter = relationships.iterator();
        TypeRelationship relationship;
        PExp relationshipConditions;
        while (!foundTrivialPath && relationshipIter.hasNext()) {
            relationship = relationshipIter.next();

            try {
                relationshipConditions =
                        pathStrategy.getValidTypeConditionsAlong(relationship,
                                value, bindings);

                foundTrivialPath = (relationshipConditions.isObviouslyTrue());

                finalConditions =
                        typeGraph.formDisjunct(relationshipConditions,
                                finalConditions);
            }
            catch (NoSolutionException nse) {}

        }
        if ( foundTrivialPath ) {
            finalConditions = typeGraph.getTrueExp();
        }
        return finalConditions;
    }

    public PExp getValidTypeConditionsTo(PExp value, MTType dst,
            Map<String, MTType> bindings) throws TypeMismatchException {

        return getValidTypeConditionsTo(value, dst, bindings, EXP_VALUE_PATH);
    }

    public PExp getValidTypeConditionsTo(MTType value, MTType dst,
            Map<String, MTType> bindings) throws TypeMismatchException {

        return getValidTypeConditionsTo(value, dst, bindings, MTTYPE_VALUE_PATH);
    }

    public static interface RelationshipPathStrategy<V> {

        public PExp getValidTypeConditionsAlong(TypeRelationship relationship,
                V value, Map<String, MTType> bindings)
                throws NoSolutionException;
    }

    public static class ExpValuePathStrategy
            implements
                RelationshipPathStrategy<PExp> {

        @Override public PExp getValidTypeConditionsAlong(
                TypeRelationship relationship, PExp value,
                Map<String, MTType> bindings) throws NoSolutionException {

            return relationship.getValidTypeConditionsTo(value, bindings);
        }
    }

    public static class MTTypeValuePathStrategy
            implements
                RelationshipPathStrategy<MTType> {

        @Override public PExp getValidTypeConditionsAlong(
                TypeRelationship relationship, MTType value,
                Map<String, MTType> bindings) throws NoSolutionException {

            return relationship.getValidTypeConditionsTo(value, bindings);
        }
    }

    void addRelationship(TypeRelationship relationship) {
        Set<TypeRelationship> bucket =
                relationships.get(relationship.getDestinationType());
        if ( bucket == null ) {
            bucket = new HashSet<TypeRelationship>();
            relationships.put(relationship.getDestinationType(), bucket);
        }
        bucket.add(relationship);
    }

    @Override public String toString() {
        StringBuffer str = new StringBuffer();

        for (Set<TypeRelationship> target : relationships.values()) {
            for (TypeRelationship rel : target) {
                str.append(rel);
            }
        }
        return str.toString();
    }
}
