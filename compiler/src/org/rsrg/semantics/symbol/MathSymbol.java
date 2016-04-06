package org.rsrg.semantics.symbol;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.DumbTypeGraph;
import org.antlr.v4.runtime.ParserRuleContext;
import org.rsrg.semantics.*;
import org.rsrg.semantics.programtype.ProgType;

import java.util.*;

public class MathSymbol extends Symbol {

    private MathClassification classification;
    private final DumbTypeGraph g;
    private final Quantification q;

    private final Map<String, MathClassification> genericsInDefiningContext =
            new HashMap<>();

    public MathSymbol(@NotNull DumbTypeGraph g, @NotNull String name,
                      @NotNull MathClassification classification) {
        this(g, name, Quantification.NONE, classification, null, ModuleIdentifier.GLOBAL);
    }

    public MathSymbol(@NotNull DumbTypeGraph g, @NotNull String name,
                      @NotNull MathClassification classification,
                      @Nullable ParserRuleContext definingTree,
                      @NotNull ModuleIdentifier moduleIdentifier) {
        this(g, name, Quantification.NONE, classification, definingTree,
                moduleIdentifier);
    }

    public MathSymbol(@NotNull DumbTypeGraph g, @NotNull String name,
                      @NotNull Quantification q,
                      @NotNull MathClassification classification,
                      @Nullable ParserRuleContext definingTree,
                      @NotNull ModuleIdentifier moduleIdentifier) {
        super(name, definingTree, moduleIdentifier);
        this.classification = classification;
        this.g = g;
        this.q = q;
    }

    public void setClassification(MathClassification n) {
        this.classification = n;
    }

    public MathClassification getClassification() {
        return classification;
    }

    public Quantification getQuantification() {
        return q;
    }

    @NotNull @Override public String getSymbolDescription() {
        return "a math symbol";
    }

    @NotNull @Override public MathSymbol toMathSymbol() {
        return this;
    }

    @NotNull @Override public Symbol instantiateGenerics(
            @NotNull Map<String, ProgType> genericInstantiations,
            @Nullable FacilitySymbol instantiatingFacility) {
        return null;
    }

}
