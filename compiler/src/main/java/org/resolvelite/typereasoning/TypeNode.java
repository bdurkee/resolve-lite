package org.resolvelite.typereasoning;

import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.semantics.MTType;
import org.resolvelite.semantics.NoSolutionException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class TypeNode {

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

        if (!myRelationships.containsKey(dst)) {
            throw TypeMismatchException.INSTANCE;
        }

        Exp finalConditions = myTypeGraph.getFalseVarExp();
        Set<TypeRelationship> relationships = myRelationships.get(dst);
        boolean foundTrivialPath = false;
        Iterator<TypeRelationship> relationshipIter = relationships.iterator();
        TypeRelationship relationship;
        Exp relationshipConditions;
        while (!foundTrivialPath && relationshipIter.hasNext()) {
            relationship = relationshipIter.next();

            try {
                relationshipConditions =
                        pathStrategy.getValidTypeConditionsAlong(relationship,
                                value, bindings);

                foundTrivialPath = (relationshipConditions.isLiteralTrue());

                finalConditions =
                        myTypeGraph.formDisjunct(relationshipConditions,
                                finalConditions);
            }
            catch (NoSolutionException nse) {}
        }

        if (foundTrivialPath) {
            finalConditions = myTypeGraph.getTrueVarExp();
        }

        return finalConditions;
    }

    public static interface RelationshipPathStrategy<V> {

        public PExp getValidTypeConditionsAlong(TypeRelationship relationship,
                                               V value, Map<String, MTType> bindings)
                throws NoSolutionException;
    }

    public static class ExpValuePathStrategy
            implements
            RelationshipPathStrategy<PExp> {

        @Override
        public PExp getValidTypeConditionsAlong(TypeRelationship relationship,
                                               PExp value,
                                               Map<String, MTType> bindings)
                throws NoSolutionException {

            return relationship.getValidTypeConditionsTo(value, bindings);
        }
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
