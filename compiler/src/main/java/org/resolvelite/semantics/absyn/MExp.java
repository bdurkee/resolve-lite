package org.resolvelite.semantics.absyn;

import org.resolvelite.semantics.MathType;

import java.util.List;

public abstract class MExp {

    private final MathType mathType, mathTypeValue;

    public MExp(MathType mathType, MathType mathTypeValue) {
        this.mathType = mathType;
        this.mathTypeValue = mathTypeValue;
    }

    public abstract List<? extends MExp> getSubExpressions();

    public abstract void setSubExpression(int index, MExp e);

    public abstract boolean isLiteral();

    public MathType getMathType() {
        return mathType;
    }

    public MathType getMathTypeValue() {
        return mathTypeValue;
    }
}
