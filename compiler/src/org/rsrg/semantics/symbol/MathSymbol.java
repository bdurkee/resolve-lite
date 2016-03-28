package org.rsrg.semantics.symbol;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.DumbTypeGraph;
import org.antlr.v4.runtime.ParserRuleContext;
import org.rsrg.semantics.*;
import org.rsrg.semantics.programtype.ProgType;

import java.util.*;

public class MathSymbol extends Symbol {

    private final MathType type;
    private final DumbTypeGraph g;
    private final Quantification q;

    private final Map<String, MathType> genericsInDefiningContext =
            new HashMap<>();

    public MathSymbol(@NotNull DumbTypeGraph g, @NotNull String name,
                      @NotNull MathType type) {
        this(g, name, Quantification.NONE, type, null, ModuleIdentifier.GLOBAL);
    }

    public MathSymbol(@NotNull DumbTypeGraph g, @NotNull String name,
                      @NotNull MathType type,
                      @Nullable ParserRuleContext definingTree,
                      @NotNull ModuleIdentifier moduleIdentifier) {
        this(g, name, Quantification.NONE, type, definingTree,
                moduleIdentifier);
    }

    public MathSymbol(@NotNull DumbTypeGraph g, @NotNull String name,
                      @NotNull Quantification q,
                      @NotNull MathType type,
                      @Nullable ParserRuleContext definingTree,
                      @NotNull ModuleIdentifier moduleIdentifier) {
        super(name, definingTree, moduleIdentifier);
        this.type = type;
        this.g = g;
        this.q = q;
    }

    public MathType getMathType() {
        return type;
    }

    public Quantification getQuantification() {
        return q;
    }

    public static List<MathType> getParameterTypes(MathFunctionType source) {
        return expandAsNeeded(source.getDomainType());
    }

    private static List<MathType> expandAsNeeded(MathType t) {
        List<MathType> result = new ArrayList<>();
        if ( t instanceof MathCartesianType) {
            MathCartesianType domainAsMTCartesian = (MathCartesianType) t;
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
