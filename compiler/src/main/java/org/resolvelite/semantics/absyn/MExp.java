package org.resolvelite.semantics.absyn;

import org.resolvelite.semantics.MTType;

import java.util.List;

public abstract class MExp {

    private final MTType mathType;
    private final MTType mathTypeValue;

    public MExp(MTType mathType, MTType mathTypeValue) {
        this.mathType = mathType;
        this.mathTypeValue = mathTypeValue;
    }

    public abstract List<? extends MExp> getSubExpressions();

    public abstract void setSubExpression(int index, MExp e);

    public abstract boolean isLiteral();

    public MTType getMathType() {
        return mathType;
    }

    public MTType getMathTypeValue() {
        return mathTypeValue;
    }
}
