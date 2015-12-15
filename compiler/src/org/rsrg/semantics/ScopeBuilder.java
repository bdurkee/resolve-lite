package org.rsrg.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.symbol.MathSymbol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * A {@code ScopeBuilder} is a working, mutable realization of {@link Scope}.
 * <p>
 * Note that {@code ScopeBuilder} has no public constructor. Instances of this
 * class can be acquired through calls to some of the methods of
 * {@link MathSymbolTable}.</p>
 */
public class ScopeBuilder extends SyntacticScope {

    @NotNull protected final List<ScopeBuilder> children = new ArrayList<>();
    @NotNull private final TypeGraph typeGraph;

    //We definitely want a linkedHashMap here for the bindings to preserve the order
    //in which entries were added to the table. Though it shouldn't necessarily
    //matter. It just does currently because of the way we grab lists of
    //formal parameters (from scope) for functions before we insert the
    //completed sym into the table.
    ScopeBuilder(@NotNull MathSymbolTable s, @NotNull TypeGraph g,
                 @Nullable ParserRuleContext definingTree,
                 @NotNull Scope parent,
                 @NotNull ModuleIdentifier moduleIdentifier) {
        super(s, definingTree, parent, moduleIdentifier, new LinkedHashMap<>());
        this.typeGraph = g;
    }

    //TODO: I think these parent and child methods can go eventually
    void setParent(@NotNull Scope parent) {
        this.parent = parent;
    }

    void addChild(@NotNull ScopeBuilder b) {
        children.add(b);
    }

    @NotNull public List<ScopeBuilder> getChildren() {
        return new ArrayList<>(children);
    }

    public MathSymbol addBinding(String name, Quantification q,
            ParserRuleContext definingTree, MTType type, MTType typeValue)
            throws DuplicateSymbolException {

        MathSymbol entry =
                new MathSymbol(typeGraph, name, q, type, typeValue,
                        definingTree, moduleIdentifier);
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
