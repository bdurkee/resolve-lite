package org.resolvelite.typereasoning;

import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.semantics.MTType;
import org.resolvelite.semantics.NoSolutionException;
import org.resolvelite.semantics.TypeMismatchException;

import java.util.*;

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
            Map<String, MTType> bindings) throws TypeMismatchException {

        if ( !relationships.containsKey(dst) ) {
            throw TypeMismatchException.INSTANCE;
        }

        PExp finalConditions = typeGraph.getFalseExp();
        Set<TypeRelationship> relationships = this.relationships.get(dst);
        boolean foundTrivialPath = false;
        Iterator<TypeRelationship> relationshipIter = relationships.iterator();
        TypeRelationship relationship;
        PExp relationshipConditions;
        /*while (!foundTrivialPath && relationshipIter.hasNext()) {
            relationship = relationshipIter.next();

            try {
                relationshipConditions =
                        pathStrategy.getValidTypeConditionsAlong(relationship,
                                value, bindings);

                foundTrivialPath = (relationshipConditions.isLiteralTrue());

                finalConditions =
                        typeGraph.formDisjunct(relationshipConditions,
                                finalConditions);
            }
            catch (NoSolutionException nse) {}
        }*/
        if ( foundTrivialPath ) {
            finalConditions = typeGraph.getTrueExp();
        }
        return finalConditions;
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
