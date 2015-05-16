package org.resolvelite.typereasoning;

import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.semantics.MTType;

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

    public MTType getTypeValue() {
        return expression.getMathTypeValue();
    }

    @Override public String toString() {
        return expression.toString();
    }
}
