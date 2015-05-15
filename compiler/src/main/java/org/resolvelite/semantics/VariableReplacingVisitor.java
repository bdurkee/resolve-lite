package org.resolvelite.semantics;

import java.util.HashMap;
import java.util.Map;

public class VariableReplacingVisitor extends MutatingVisitor {
    private final Map<String, MTType> substitutions;

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
}
