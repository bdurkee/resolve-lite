package edu.clemson.resolve.semantics.symbol;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSelector;
import edu.clemson.resolve.proving.absyn.PSymbol;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import edu.clemson.resolve.semantics.DumbMathClssftnHandler;
import edu.clemson.resolve.semantics.ModuleIdentifier;
import org.antlr.v4.runtime.ParserRuleContext;
import edu.clemson.resolve.semantics.programtype.PTRepresentation;
import edu.clemson.resolve.semantics.programtype.ProgType;

import java.util.Map;

public class ProgReprTypeSymbol extends Symbol {

    /**
     * A backing field for our (type model) {@code definition} parameter. This
     * might be {@code null} in the case where we represent a facility bound
     * type representation.
     */
    @Nullable
    protected final TypeModelSymbol definition;

    /**
     * These annotation exprs should never be {@code null}; use just
     * {@code true} instead.
     */
    @NotNull
    protected final PExp convention, correspondence;

    @NotNull
    protected final DumbMathClssftnHandler typeGraph;
    @NotNull
    protected final PTRepresentation representation;

    public ProgReprTypeSymbol(@NotNull DumbMathClssftnHandler g,
                              @NotNull String name,
                              @Nullable ParserRuleContext definingElement,
                              @NotNull ModuleIdentifier moduleIdentifier,
                              @Nullable TypeModelSymbol definition,
                              @NotNull PTRepresentation representation,
                              @NotNull PExp convention,
                              @NotNull PExp correspondence) {
        super(name, definingElement, moduleIdentifier);
        this.definition = definition;
        this.representation = representation;
        this.convention = convention;
        this.correspondence = correspondence;
        this.typeGraph = g;
    }

    @NotNull
    public PSymbol exemplarAsPSymbol(boolean incoming) {
        return new PSymbol.PSymbolBuilder(representation.getExemplarName())
                .mathClssfctn(representation.toMath()).incoming(incoming).build();
    }

    @NotNull
    public PSymbol exemplarAsPSymbol() {
        return exemplarAsPSymbol(false);
    }

    @NotNull
    public PSelector conceptualExemplarAsPSymbol(boolean incoming) {
        return new PSelector(
                new PSymbol.PSymbolBuilder("conc").mathClssfctn(typeGraph.BOOLEAN)
                        .incoming(incoming).build(),
                new PSymbol.PSymbolBuilder(representation.getExemplarName())
                        .mathClssfctn(representation.toMath()).build());
    }

    @NotNull
    public PSelector conceptualExemplarAsPSymbol() {
        return conceptualExemplarAsPSymbol(false);
    }

    @NotNull
    public PTRepresentation getRepresentationType() {
        return representation;
    }

    @Nullable
    public TypeModelSymbol getDefinition() {
        return definition;
    }

    @NotNull
    public PExp getConvention() {
        return convention;
    }

    @NotNull
    public PExp getCorrespondence() {
        return correspondence;
    }

    @NotNull
    @Override
    public ProgTypeSymbol toProgTypeSymbol() {
        return new ProgTypeSymbol(typeGraph, getName(), representation,
                (definition == null) ? null : definition.modelType,
                getDefiningTree(), getModuleIdentifier());
    }

    @NotNull
    @Override
    public MathClssftnWrappingSymbol toMathSymbol() {
        return toProgTypeSymbol().toMathSymbol();
    }

    @NotNull
    @Override
    public ProgReprTypeSymbol toProgReprTypeSymbol() {
        return this;
    }

    @NotNull
    @Override
    public String getSymbolDescription() {
        return "a program type representation definition";
    }

    @Override
    public String toString() {
        return getName();
    }

    @NotNull
    @Override
    public Symbol instantiateGenerics(
            @NotNull Map<String, ProgType> genericInstantiations,
            @Nullable FacilitySymbol instantiatingFacility) {

        //type representations are an internal implementation detail of
        //some realization and shouldn't be accessible through a facility
        //instantiation
        throw new UnsupportedOperationException("Cannot instantiate "
                + this.getClass());
    }

}
