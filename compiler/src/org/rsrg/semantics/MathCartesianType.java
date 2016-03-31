package org.rsrg.semantics;

import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.compiler.ErrorManager;
import edu.clemson.resolve.misc.Utils;

import java.util.*;

public class MathCartesianType extends MathType {

    private final List<MathType> components = new LinkedList<>();

    public MathCartesianType(DumbTypeGraph g, List<MathType> componentTypes) {
        super(g, g.INVALID);
        components.addAll(componentTypes);
        //I'm type permissible if each component is type permissible.. right?
    }

    @Override public List<MathType> getComponentTypes() {
        return components;
    }

    @Override public MathType withVariablesSubstituted(
            Map<MathType, MathType> substitutions) {
        List<MathType> newComponents = new ArrayList<>();
        for (MathType component : components) {
            newComponents.add(
                    component.withVariablesSubstituted(substitutions));
        }
        return new MathCartesianType(g, newComponents);
    }

    @Override public String toString() {
        return "(" + Utils.join(components, " * ") + ")";
    }
}
