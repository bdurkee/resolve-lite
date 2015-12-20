
package org.rsrg.semantics;

import java.util.*;

public class BoundVariableVisitor extends TypeVisitor {

    private Deque<Map<String, BindingInfo>> boundVariables = new LinkedList<>();

    public MTType getInnermostBinding(String name) {
        return getInnermostBindingInfo(name).type;
    }

    public void annotateInnermostBinding(String name, Object key, Object value) {

        getInnermostBindingInfo(name).annotations.put(key, value);
    }

    public Object getInnermostBindingAnnotation(String name, Object key) {
        return getInnermostBindingInfo(name).annotations.get(key);
    }

    private BindingInfo getInnermostBindingInfo(String name) {
        BindingInfo binding = null;
        Iterator<Map<String, BindingInfo>> scopes = boundVariables.iterator();
        while (binding == null && scopes.hasNext()) {
            binding = scopes.next().get(name);
        }
        if ( binding == null ) {
            throw new NoSuchElementException();
        }
        return binding;
    }

    protected Map<String, BindingInfo>
    toBindingInfoMap(Map<String, MTType> vars) {
        Map<String, BindingInfo> result = new HashMap<>();

        for (Map.Entry<String, MTType> entry : vars.entrySet()) {
            result.put(entry.getKey(), new BindingInfo(entry.getValue()));
        }

        return result;
    }

    protected class BindingInfo {
        public final MTType type;
        public final Map<Object, Object> annotations;

        public BindingInfo(MTType type) {
            this.type = type;
            this.annotations = new HashMap<Object, Object>();
        }
    }
}