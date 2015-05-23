package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.semantics.Quantification;
import org.resolvelite.semantics.programtype.PTInvalid;
import org.resolvelite.semantics.programtype.PTType;

import java.util.Map;

public class ProgVariableSymbol extends Symbol {

    private final PTType type;
    private final MathSymbol mathSymbolAlterEgo;

    public ProgVariableSymbol(String name, ParseTree definingTree, PTType type,
            String moduleID) {
        super(name, definingTree, moduleID);
        this.type = type;
        this.mathSymbolAlterEgo =
                new MathSymbol(type.getTypeGraph(), name, Quantification.NONE,
                        type.toMath(), null, definingTree, moduleID);
    }

    public PTType getProgramType() {
        return type;
    }

    @Override public String getSymbolDescription() {
        return "a program variable";
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
