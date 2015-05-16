package org.resolvelite.semantics;

import org.resolvelite.typereasoning.TypeGraph;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class VariableReplacingVisitor extends MutatingVisitor {
    private final Map<String, MTType> substitutions;

    public VariableReplacingVisitor(Map<String, String> substitutions,
            TypeGraph g) {
        this.substitutions = convertToMTNamedMap(substitutions, g);
    }

    public VariableReplacingVisitor(Map<String, MTType> substitutions) {
        this.substitutions = new HashMap<String, MTType>(substitutions);
    }

    private static Map<String, MTType> convertToMTNamedMap(
            Map<String, String> original, TypeGraph g) {
        Map<String, MTType> result = new HashMap<>();

        for (Map.Entry<String, String> entry : original.entrySet()) {
            result.put(entry.getKey(), new MTNamed(g, entry.getValue()));
        }
        return result;
    }

    @Override public void endMTNamed(MTNamed t) {
        if ( substitutions.containsKey(t.name) ) {
            try {
                getInnermostBinding(t.name);
                //This is bound to some inner scope
            }
            catch (NoSuchElementException e) {
                replaceWith(substitutions.get(t.name));
            }
        }
    }
}
