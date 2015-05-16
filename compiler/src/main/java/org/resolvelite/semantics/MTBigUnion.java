package org.resolvelite.semantics;

import org.resolvelite.typereasoning.TypeGraph;

import java.util.*;

public class MTBigUnion extends MTAbstract<MTBigUnion> {

    private final MTType expression;
    private TreeMap<String, MTType> quantifiedVariables;

    private final Map<Integer, String> componentIndices = new HashMap<>();
    private List<MTType> components;

    /**
     * If {@code quantifiedVariables} is {@code null}, then this is undefined.
     */
    private final int uniqueQuantifiedVariableCount;

    public MTBigUnion(TypeGraph g, Map<String, MTType> quantifiedVariables,
            MTType expression) {
        super(g);
        this.quantifiedVariables =
                new TreeMap<String, MTType>(quantifiedVariables);
        this.uniqueQuantifiedVariableCount = -1;
        this.expression = expression;
    }

    public MTType getExpression() {
        return expression;
    }

    public Map<String, MTType> getQuantifiedVariables() {
        return quantifiedVariables;
    }

    @Override public List<? extends MTType> getComponentTypes() {
        return new ArrayList<>();
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
                getTypeGraph().CLS.accept(v);
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

    @Override public MTType withComponentReplaced(int index, MTType newType) {
        //ensureQuantifiedTypes();

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

    @Override public void acceptClose(TypeVisitor v) {
        v.endMTBigUnion(this);
        v.endMTAbstract(this);
        v.endMTType(this);
    }
}
