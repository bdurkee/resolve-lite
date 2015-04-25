package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.semantics.programtype.PTInvalid;
import org.resolvelite.semantics.programtype.PTType;

public class ProgVariableSymbol extends Symbol {

    private PTType type;
    private final MathSymbol mathSymbolAlterEgo;

    public ProgVariableSymbol(String name, ParseTree definingTree,
            String moduleID, PTType type) {
        super(name, definingTree, moduleID);
        this.type = type;
        this.mathSymbolAlterEgo =
                new MathSymbol(type.getTypeGraph(), name, Quantification.NONE,
                        type.toMath(), null, definingTree, moduleID);
    }

    @Override public String getEntryTypeDescription() {
        return "a program variable";
    }

    @Override public MathSymbol toMathSymbol() {
        mathSymbolAlterEgo.setTypes(type.toMath(), null);
        return mathSymbolAlterEgo;
    }
}
