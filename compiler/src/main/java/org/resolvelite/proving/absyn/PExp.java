package org.resolvelite.proving.absyn;

import org.resolvelite.semantics.MTType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class PExp {

    private MTType type, typeValue;

    public PExp(MTType type, MTType typeValue) {
        this.type = type;
        this.typeValue = typeValue;
    }

    public final MTType getMathType() {
        return type;
    }

    public final MTType getMathTypeValue() {
        return typeValue;
    }

    public abstract PExp substitute(Map<PExp, PExp> substitutions);

    public abstract boolean containsName(String name);

    public abstract List<PExp> getSubExpressions();

    public abstract boolean isObviouslyTrue();

    public abstract boolean isVariable();

    public abstract boolean isLiteral();

    public abstract boolean isFunction();

    public final List<PExp> splitIntoConjuncts() {
        List<PExp> conjuncts = new ArrayList<>();
        splitIntoConjuncts(conjuncts);
        return conjuncts;
    }

    protected abstract void splitIntoConjuncts(List<PExp> accumulator);
}
