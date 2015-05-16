package org.resolvelite.semantics;

import org.resolvelite.typereasoning.TypeGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MTBigUnion extends MTAbstract<MTBigUnion> {

    private final MTType expression;
    private TreeMap<String, MTType> quantifiedVariables;

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

    @Override
    public List<? extends MTType> getComponentTypes() {
        return new ArrayList<>();
    }

    @Override
    public void acceptOpen(TypeVisitor v) {
        v.beginMTType(this);
        v.beginMTAbstract(this);
        v.beginMTBigUnion(this);
    }

    @Override
    public void accept(TypeVisitor v) {
        acceptOpen(v);
        v.beginChildren(this);

        if (quantifiedVariables == null) {
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

    @Override
    public void acceptClose(TypeVisitor v) {
        v.endMTBigUnion(this);
        v.endMTAbstract(this);
        v.endMTType(this);
    }
}
