package org.rsrg.semantics.symbol;

import org.antlr.v4.runtime.ParserRuleContext;
import org.rsrg.semantics.MTType;
import org.rsrg.semantics.Quantification;
import org.rsrg.semantics.programtype.PTType;

import java.util.Map;

public class ProgVariableSymbol extends Symbol implements ModuleParameterizableSymbol {

    private final PTType type;
    private final MathSymbol mathSymbolAlterEgo;

    public ProgVariableSymbol(String name,
                              ParserRuleContext definingTree, PTType type,
            String moduleID) {
        super(name, definingTree, moduleID);
        this.type = type;
        this.mathSymbolAlterEgo =
                new MathSymbol(type.getTypeGraph(), name, Quantification.NONE,
                        type.toMath(), null, definingTree, moduleID);
    }

    @Override public PTType getProgramType() {
        return type;
    }

    @Override public String getSymbolDescription() {
        return "a program variable";
    }

    @Override public MTType getMathType() {
        return mathSymbolAlterEgo.getType();
    }

    @Override public ProgVariableSymbol toProgVariableSymbol() {
        return this;
    }

    @Override public MathSymbol toMathSymbol() {
        return mathSymbolAlterEgo;
    }

    @Override public Symbol instantiateGenerics(
            Map<String, PTType> genericInstantiations,
            FacilitySymbol instantiatingFacility) {

        Symbol result;
        PTType instantiatedType =
                type.instantiateGenerics(genericInstantiations,
                        instantiatingFacility);

        if ( instantiatedType != type ) {
            result =
                    new ProgVariableSymbol(getName(), getDefiningTree(),
                            instantiatedType, getModuleID());
        }
        else {
            result = this;
        }
        return result;
    }

}
