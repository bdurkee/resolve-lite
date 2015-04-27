package org.resolvelite.semantics;

import org.resolvelite.typereasoning.TypeGraph;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a type that is simply a named reference to some bound variable.
 * For example, in BigUnion{t : MType}{t}, the second "t" is a named type.
 */
public class MTNamed extends MTType {

    private final static int BASE_HASH = "MTNamed".hashCode();

    public final String name;

    public MTNamed(TypeGraph g, String name) {
        super(g);
        this.name = name;
    }

    @SuppressWarnings("unchecked") @Override public List<MTType>
            getComponentTypes() {
        return (List<MTType>) Collections.EMPTY_LIST;
    }

    @Override public String toString() {
        return "'" + name + "'";
    }
}