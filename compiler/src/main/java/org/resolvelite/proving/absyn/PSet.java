package org.resolvelite.proving.absyn;

import org.resolvelite.misc.Utils;
import org.resolvelite.semantics.MTType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PSet extends PExp {

    private final List<PExp> elements = new ArrayList<>();

    //Todo: hash correctly.
    public PSet(MTType type, MTType typeValue, List<PExp> elements) {
        super(0, 0, type, typeValue);
        this.elements.addAll(elements);
    }

    @Override public PExp substitute(Map<PExp, PExp> substitutions) {
        return null;
    }

    @Override public boolean containsName(String name) {
        return elements.stream()
                .filter(u -> u.containsName(name))
                .collect(Collectors.toList()).isEmpty();
    }

    @Override public List<PExp> getSubExpressions() {
        return elements;
    }

    @Override public boolean isLiteralTrue() {
        return false;
    }

    @Override public boolean isLiteralFalse() {
        return false;
    }

    @Override public boolean isVariable() {
        return false;
    }

    @Override public boolean isLiteral() {
        return false;
    }

    @Override public boolean isFunction() {
        return false;
    }

    @Override public PExp copy() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override protected void splitIntoConjuncts(List<PExp> accumulator) {

    }

    @Override public String toString() {
        return "{" + Utils.join(elements, ", ") + "}";
    }
}
