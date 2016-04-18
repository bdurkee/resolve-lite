package org.rsrg.semantics.symbol;

import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.MathClassification;
import org.rsrg.semantics.MathNamedClassification;
import org.rsrg.semantics.ModuleIdentifier;
import org.rsrg.semantics.Quantification;
import org.rsrg.semantics.programtype.ProgType;

import java.util.Map;

public class ProgVariableSymbol extends Symbol {

    private final ProgType type;
    @NotNull private final MathClssftnWrappingSymbol mathSymbolAlterEgo;

    public ProgVariableSymbol(@NotNull String name,
                              @Nullable ParserRuleContext definingTree,
                              @NotNull ProgType type,
                              @NotNull ModuleIdentifier moduleIdentifier) {
        super(name, definingTree, moduleIdentifier);
        this.type = type;
        MathClassification m = type.toMath();
        this.mathSymbolAlterEgo =
                new MathClssftnWrappingSymbol(type.getTypeGraph(), name, Quantification.NONE,
                        new MathNamedClassification(type.getTypeGraph(),
                                name, m.typeRefDepth-1, m), definingTree,
                        moduleIdentifier);
    }

    @NotNull public ProgType getProgramType() {
        return type;
    }

    @NotNull @Override public String getSymbolDescription() {
        return "a program variable";
    }

    @NotNull @Override public ProgVariableSymbol toProgVariableSymbol() {
        return this;
    }

    @NotNull @Override public MathClssftnWrappingSymbol toMathSymbol() {
        return mathSymbolAlterEgo;
    }

    @NotNull @Override public Symbol instantiateGenerics(
            @NotNull Map<String, ProgType> genericInstantiations,
            @Nullable FacilitySymbol instantiatingFacility) {

        Symbol result;
        ProgType instantiatedType =
                type.instantiateGenerics(genericInstantiations,
                        instantiatingFacility);

        if ( instantiatedType != type ) {
            result =
                    new ProgVariableSymbol(getName(), getDefiningTree(),
                            instantiatedType, getModuleIdentifier());
        }
        else {
            result = this;
        }
        return result;
    }

}
