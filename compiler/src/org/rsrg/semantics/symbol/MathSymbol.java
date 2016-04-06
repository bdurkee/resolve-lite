package org.rsrg.semantics.symbol;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.DumbTypeGraph;
import org.antlr.v4.runtime.ParserRuleContext;
import org.rsrg.semantics.*;
import org.rsrg.semantics.programtype.ProgType;

import java.util.*;

public class MathSymbol extends Symbol {

    private final MathClassification type;
    private final DumbTypeGraph g;
    private final Quantification q;

    private final Map<String, MathClassification> genericsInDefiningContext =
            new HashMap<>();

    public MathSymbol(@NotNull DumbTypeGraph g, @NotNull String name,
                      @NotNull MathClassification type) {
        this(g, name, Quantification.NONE, type, null, ModuleIdentifier.GLOBAL);
    }

    public MathSymbol(@NotNull DumbTypeGraph g, @NotNull String name,
                      @NotNull MathClassification type,
                      @Nullable ParserRuleContext definingTree,
                      @NotNull ModuleIdentifier moduleIdentifier) {
        this(g, name, Quantification.NONE, type, definingTree,
                moduleIdentifier);
    }

    public MathSymbol(@NotNull DumbTypeGraph g, @NotNull String name,
                      @NotNull Quantification q,
                      @NotNull MathClassification type,
                      @Nullable ParserRuleContext definingTree,
                      @NotNull ModuleIdentifier moduleIdentifier) {
        super(name, definingTree, moduleIdentifier);
        this.type = type;
        this.g = g;
        this.q = q;
    }

    public MathClassification getMathType() {
        return type;
    }
    /*public MathClassification getMathType() {
        return type.getEnclosingClassification();
    }

    public MathClassification getExactMathType() {
        return type
    }*/
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
