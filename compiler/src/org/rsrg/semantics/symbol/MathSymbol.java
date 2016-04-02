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

    public Quantification getQuantification() {
        return q;
    }

    public static List<MathClassification> getParameterTypes(MathArrowClassification source) {
        return expandAsNeeded(source.getDomainType());
    }

    private static List<MathClassification> expandAsNeeded(MathClassification t) {
        List<MathClassification> result = new ArrayList<>();
        if ( t instanceof MathCartesianClassification) {
            MathCartesianClassification domainAsMTCartesian = (MathCartesianClassification) t;
            result.addAll(domainAsMTCartesian.getComponentTypes());
            /*for (int i = 0; i < domainAsMTCartesian.size(); i++) {
                result.add(domainAsMTCartesian.getFactor(i));
            }*/
        }
        else {
            if ( !(t == t.getTypeGraph().VOID) ) {
                result.add(t);
            }
        }
        return result;
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
