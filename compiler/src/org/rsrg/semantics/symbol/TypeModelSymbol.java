package org.rsrg.semantics.symbol;

import edu.clemson.resolve.parser.ResolveParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.DumbTypeGraph;
import org.rsrg.semantics.MathType;
import org.rsrg.semantics.ModuleIdentifier;
import org.antlr.v4.runtime.ParserRuleContext;
import org.rsrg.semantics.programtype.PTFamily;

/** Describes a "Type family .." introduction as would be found in an
 *  {@link ResolveParser.ConceptModuleDeclContext} or
 *  {@link ResolveParser.ConceptExtensionModuleDeclContext}
 */
public class TypeModelSymbol extends ProgTypeSymbol {

    @NotNull private final MathSymbol exemplar;

    public TypeModelSymbol(@NotNull DumbTypeGraph g, @NotNull String name,
                           @NotNull MathType modelType,
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

    @NotNull @Override public TypeModelSymbol toTypeModelSymbol() {
        return this;
    }
}
