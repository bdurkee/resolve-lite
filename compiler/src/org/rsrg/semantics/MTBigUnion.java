package org.rsrg.semantics;

import edu.clemson.resolve.typereasoning.TypeGraph;

import java.util.*;

/**
 * A constructed type consisting of the union over one or more quantified
 * types. For example {@code BigUnion t, r : MType}{t intersect r}} is the
 * type of all intersections.
 */
public class MTBigUnion extends MTAbstract<MTBigUnion> {

    private static final int BASE_HASH = "MTBigUnion".hashCode();

    private TreeMap<String, MTType> quantifiedVariables;

    /**
     * If {@code quantifiedVariables} is {@code null}, then
     * {@code uniqueQuantifiedVariableCount} is undefined.
     */
    private final int uniqueQuantifiedVariableCount;

    private final MTType expression;

    private final Map<Integer, String> componentIndices = new HashMap<>();
    private List<MTType> myComponents;

    public MTBigUnion(TypeGraph g, Map<String, MTType> quantifiedVariables,
            MTType expression) {
        super(g);

        this.quantifiedVariables =
                new TreeMap<String, MTType>(quantifiedVariables);
        this.uniqueQuantifiedVariableCount = -1;
        this.expression = expression;
    }

    /**
     * This provides a small optimization for working with
     * {@link SyntacticSubtypeChecker SyntacticSubtypeChecker}. In the case
     * where we're just going to haven n variables whose names are
     * meant to be guaranteed not to appear in {@code expression}s, we just
     * pass in the number of variables this union is meant to be quantified over
     * rather than going through the trouble of giving them names and types and
     * putting them in a map.
     */
    MTBigUnion(TypeGraph g, int uniqueVariableCount, MTType expression) {
        super(g);
        this.quantifiedVariables = null;
        this.uniqueQuantifiedVariableCount = uniqueVariableCount;
        this.expression = expression;
    }

    public MTType getExpression() {
        return expression;
    }

    public int getQuantifiedVariablesSize() {
        int result;
        if ( quantifiedVariables == null ) {
            result = uniqueQuantifiedVariableCount;
        }
        else {
            result = quantifiedVariables.size();
        }
        return result;
    }

    public Map<String, MTType> getQuantifiedVariables() {
        ensureQuantifiedTypes();
        return quantifiedVariables;
    }

    @Override public void acceptOpen(TypeVisitor v) {
        v.beginMTType(this);
        v.beginMTAbstract(this);
        v.beginMTBigUnion(this);
    }

    @Override public void accept(TypeVisitor v) {
        acceptOpen(v);
        v.beginChildren(this);

        if ( quantifiedVariables == null ) {
            for (int i = 0; i < uniqueQuantifiedVariableCount; i++) {
                myTypeGraph.MTYPE.accept(v);
            }
        }
        else {
            for (MTType t : quantifiedVariables.values()) {
                t.accept(v);
            }
        }
        expression.accept(v);
        v.endChildren(this);
        acceptClose(v);
    }

    @Override public void acceptClose(TypeVisitor v) {
        v.endMTBigUnion(this);
        v.endMTAbstract(this);
        v.endMTType(this);
    }

    @Override public List<MTType> getComponentTypes() {
        if ( myComponents == null ) {
            if ( quantifiedVariables == null ) {
                myComponents =
                        new ArrayList<MTType>(uniqueQuantifiedVariableCount);

                for (int i = 0; i < uniqueQuantifiedVariableCount; i++) {
                    myComponents.add(myTypeGraph.MTYPE);
                }
            }
            else {
                List<MTType> components =
                        new ArrayList<MTType>(quantifiedVariables.size());
                for (Map.Entry<String, MTType> entry : quantifiedVariables
                        .entrySet()) {

                    componentIndices.put(components.size(), entry.getKey());
                    components.add(entry.getValue());
                }
                components.add(expression);
                myComponents = Collections.unmodifiableList(components);
            }
        }
        return myComponents;
    }

    @Override public MTType withComponentReplaced(int index, MTType newType) {
        ensureQuantifiedTypes();

        Map<String, MTType> newQuantifiedVariables;
        MTType newExpression;

        if ( index < quantifiedVariables.size() ) {
            newQuantifiedVariables =
                    new HashMap<String, MTType>(quantifiedVariables);

            newQuantifiedVariables.put(componentIndices.get(index), newType);

            newExpression = expression;
        }
        else if ( index == quantifiedVariables.size() ) {
            newQuantifiedVariables = quantifiedVariables;

            newExpression = newType;
        }
        else {
            throw new IndexOutOfBoundsException();
        }
        return new MTBigUnion(getTypeGraph(), newQuantifiedVariables,
                newExpression);
    }

    @Override public int getHashCode() {
        ensureQuantifiedTypes();
        int result = BASE_HASH;

        //Note that order of these MTTypes doesn't matter
        for (MTType t : quantifiedVariables.values()) {
            result += t.hashCode();
        }
        result *= 57;
        result += expression.hashCode();
        return result;
    }

    @Override public String toString() {
        ensureQuantifiedTypes();
        return "BigUnion" + quantifiedVariables + "{" + expression + "}";
    }

    /**
     * Converts us from a "enh, some number of unique variables" big union to
     * a "specific named unique variables" big union if one of the methods is
     * called that requires such a thing.
     */
    private void ensureQuantifiedTypes() {
        if ( quantifiedVariables == null ) {
            quantifiedVariables = new TreeMap<String, MTType>();

            for (int i = 0; i < uniqueQuantifiedVariableCount; i++) {
                quantifiedVariables.put("*" + i, myTypeGraph.MTYPE);
            }
        }
    }
}
