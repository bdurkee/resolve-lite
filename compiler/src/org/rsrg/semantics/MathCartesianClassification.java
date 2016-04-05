package org.rsrg.semantics;

import edu.clemson.resolve.misc.Utils;

import java.util.*;

public class MathCartesianClassification extends MathClassification {

    private final List<MathClassification> components = new LinkedList<>();

    public MathCartesianClassification(DumbTypeGraph g, List<MathClassification> componentTypes) {
        super(g, g.INVALID);
        components.addAll(componentTypes);
        //I'm type permissible if each component is type permissible.. right?
    }

    @Override public List<MathClassification> getComponentTypes() {
        return components;
    }

    @Override public MathClassification withVariablesSubstituted(
            Map<MathClassification, MathClassification> substitutions) {
        List<MathClassification> newComponents = new ArrayList<>();
        for (MathClassification component : components) {
            newComponents.add(
                    component.withVariablesSubstituted(substitutions));
        }
        return new MathCartesianClassification(g, newComponents);
    }

    public int size() {
        return components.size();
    }

    public MathClassification getFactor(int i) {
        return components.get(i);
    }

    @Override public String toString() {
        return "(" + Utils.join(components, " * ") + ")";
    }
}
