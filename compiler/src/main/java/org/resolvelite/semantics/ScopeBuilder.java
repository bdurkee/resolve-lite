package org.resolvelite.semantics;

import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.semantics.symbol.MathSymbol;
import org.resolvelite.semantics.symbol.Symbol;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * A {@code ScopeBuilder} is a working, mutable realization of {@link Scope}.
 * 
 * Note that {@code ScopeBuilder} has no public constructor. Instances of this
 * class can be acquired through calls to some of the methods of
 * {@link SymbolTable}.
 */
public class ScopeBuilder extends SyntacticScope {

    protected final List<ScopeBuilder> children = new ArrayList<>();
    private final TypeGraph typeGraph;

    //Todo: We definitely want a linkedHashMap here to preserve the order
    //in which entries were added to the table. Though it shouldn't necessarily
    //matter. It just does currently because of the way we grab lists of
    //formal parameters (from scope) for functions before we insert the
    //completed sym into the table.
    ScopeBuilder(SymbolTable s, TypeGraph g, ParseTree definingTree,
            Scope parent, String moduleID) {
        super(s, definingTree, parent, moduleID, new LinkedHashMap<>());
        this.typeGraph = g;
    }

    void setParent(Scope parent) {
        this.parent = parent;
    }

    void addChild(ScopeBuilder b) {
        children.add(b);
    }

    public List<ScopeBuilder> getChildren() {
        return new ArrayList<>(children);
    }

    public MathSymbol addBinding(String name, Quantification q,
            ParseTree definingTree, MTType type, MTType typeValue)
            throws DuplicateSymbolException {

        MathSymbol entry =
                new MathSymbol(typeGraph, name, q, type, typeValue,
                        definingTree, moduleID);
        symbols.put(name, entry);
        return entry;
    }

    public MathSymbol addBinding(String name, Quantification q,
            ParseTree definingTree, MTType type)
            throws DuplicateSymbolException {
        return addBinding(name, q, definingTree, type, null);
    }

    public MathSymbol addBinding(String name, ParseTree definingTree,
            MTType type, MTType typeValue) throws DuplicateSymbolException {
        return addBinding(name, Quantification.NONE, definingTree, type,
                typeValue);
    }

    public MathSymbol addBinding(String name, ParseTree definingTree,
            MTType type) throws DuplicateSymbolException {
        return addBinding(name, Quantification.NONE, definingTree, type);
    }
}
