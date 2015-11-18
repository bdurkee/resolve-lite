package org.rsrg.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.rsrg.semantics.symbol.MathSymbol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * A {@code ScopeBuilder} is a working, mutable realization of {@link Scope}.
 * <p>
 * Note that {@code ScopeBuilder} has no public constructor. Instances of this
 * class can be acquired through calls to some of the methods of
 * {@link MathSymbolTableBuilder}.</p>
 */
public class ScopeBuilder extends SyntacticScope {

    protected final List<ScopeBuilder> children = new ArrayList<>();
    private final TypeGraph typeGraph;

    //Todo: We definitely want a linkedHashMap here to preserve the order
    //in which entries were added to the table. Though it shouldn't necessarily
    //matter. It just does currently because of the way we grab lists of
    //formal parameters (from scope) for functions before we insert the
    //completed sym into the table.
    ScopeBuilder(MathSymbolTableBuilder s, TypeGraph g, ParserRuleContext definingTree,
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
            ParserRuleContext definingTree, MTType type, MTType typeValue)
            throws DuplicateSymbolException {

        MathSymbol entry =
                new MathSymbol(typeGraph, name, q, type, typeValue,
                        definingTree, moduleID);
        symbols.put(name, entry);
        return entry;
    }

    public MathSymbol addBinding(String name, Quantification q,
                                 ParserRuleContext definingTree, MTType type)
            throws DuplicateSymbolException {
        return addBinding(name, q, definingTree, type, null);
    }

    public MathSymbol addBinding(String name, ParserRuleContext definingTree,
            MTType type, MTType typeValue) throws DuplicateSymbolException {
        return addBinding(name, Quantification.NONE, definingTree, type,
                typeValue);
    }

    public MathSymbol addBinding(String name, ParserRuleContext definingTree,
            MTType type) throws DuplicateSymbolException {
        return addBinding(name, Quantification.NONE, definingTree, type);
    }
}
