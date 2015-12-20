package org.rsrg.semantics.symbol;

import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.ModuleIdentifier;
import org.rsrg.semantics.Quantification;
import org.rsrg.semantics.programtype.PTType;

import java.util.Map;

public class ProgVariableSymbol extends Symbol {

    private final PTType type;
    @NotNull private final MathSymbol mathSymbolAlterEgo;

    public ProgVariableSymbol(@NotNull String name,
                              @Nullable ParserRuleContext definingTree,
                              @NotNull PTType type,
                              @NotNull ModuleIdentifier moduleIdentifier) {
        super(name, definingTree, moduleIdentifier);
        this.type = type;
        this.mathSymbolAlterEgo =
                new MathSymbol(type.getTypeGraph(), name, Quantification.NONE,
                        type.toMath(), null, definingTree, moduleIdentifier);
    }

    @NotNull public PTType getProgramType() {
        return type;
    }

    @NotNull @Override public String getSymbolDescription() {
        return "a program variable";
    }

    @NotNull @Override public ProgVariableSymbol toProgVariableSymbol() {
        return this;
    }

    @NotNull @Override public MathSymbol toMathSymbol() {
        return mathSymbolAlterEgo;
    }

    @NotNull @Override public Symbol instantiateGenerics(
            @NotNull Map<String, PTType> genericInstantiations,
            @Nullable FacilitySymbol instantiatingFacility) {

        Symbol result;
        PTType instantiatedType =
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
