package org.rsrg.semantics.symbol;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.ModuleIdentifier;
import org.rsrg.semantics.TypeGraph;
import org.antlr.v4.runtime.ParserRuleContext;
import org.rsrg.semantics.MTType;
import org.rsrg.semantics.programtype.PTFamily;

/**
 * Describes a "Type family .." introduction as would be found in a concept or
 * enhancement module.
 */
public class TypeModelSymbol extends ProgTypeSymbol {

    @NotNull private final MathSymbol exemplar;

    public TypeModelSymbol(@NotNull TypeGraph g, @NotNull String name,
                           @NotNull MTType modelType,
                           @NotNull PTFamily programType,
                           @NotNull MathSymbol exemplar,
                           @Nullable ParserRuleContext definingTree,
                           @NotNull ModuleIdentifier moduleIdentifier) {
        super(g, name, programType, modelType, definingTree, moduleIdentifier);
        this.exemplar = exemplar;
    }

    @NotNull public MathSymbol getExemplar() {
        return exemplar;
    }

    @NotNull @Override public PTFamily getProgramType() {
        return (PTFamily) super.getProgramType();
    }

    @NotNull @Override public TypeModelSymbol toProgTypeModelSymbol() {
        return this;
    }
}
