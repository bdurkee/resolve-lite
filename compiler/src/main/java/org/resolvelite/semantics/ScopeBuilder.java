package org.resolvelite.semantics;

import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.semantics.symbol.Symbol;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@code ScopeBuilder} is a working, mutable realization of {@link Scope}.
 *
 * <p>Note that {@code ScopeBuilder} has no public constructor.
 * Instances of this class can be acquired through calls to some of the methods
 * of {@link SymbolTable}.
 */
public class ScopeBuilder extends BaseScope {

    private final TypeGraph typeGraph;

    ScopeBuilder(SymbolTable s, TypeGraph g, ParseTree definingTree,
            Scope parent, String moduleID) {
        super(s, definingTree, parent, moduleID, new HashMap<>());
        this.typeGraph = g;
    }

    void setParent(Scope parent) {
        this.parent = parent;
    }

}
