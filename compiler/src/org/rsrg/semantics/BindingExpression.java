package org.rsrg.semantics;

import edu.clemson.resolve.proving.absyn.PExp;

import java.util.Map;

public class BindingExpression {

    private final TypeGraph typeGraph;
    private PExp expression;

    public BindingExpression(TypeGraph g, PExp expression) {
        this.expression = expression;
        this.typeGraph = g;
    }

    public MTType getType() {
        return expression.getMathType();
    }

    public TypeGraph getTypeGraph() {
        return typeGraph;
    }

    public MTType getTypeValue() {
        return expression.getMathTypeValue();
    }

    @Override public String toString() {
        return expression.toString();
    }

    private MTType getTypeUnderBinding(MTType original,
            Map<String, MTType> typeBindings) {
        return original.getCopyWithVariablesSubstituted(typeBindings);
    }
}
