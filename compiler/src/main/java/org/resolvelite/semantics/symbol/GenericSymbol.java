package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.semantics.MTNamed;
import org.resolvelite.semantics.MTType;
import org.resolvelite.semantics.programtype.PTElement;
import org.resolvelite.semantics.programtype.PTGeneric;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.typereasoning.TypeGraph;

public class GenericSymbol extends Symbol {
    private final PTType type;
    private final TypeGraph g;
    private final MathSymbol mathSymbolAlterEgo;

    public GenericSymbol(TypeGraph g, String name, ParseTree definingTree,
            String moduleID) {
        super(name, definingTree, moduleID);
        this.g = g;
        this.type = new PTElement(g);

        MTType typeValue =
                new PTGeneric(type.getTypeGraph(), getName()).toMath();
        mathSymbolAlterEgo =
                new MathSymbol(g, name, Quantification.NONE, type.toMath(),
                        typeValue, definingTree, moduleID);
    }

    @Override public String getEntryTypeDescription() {
        return "a generic";
    }

    @Override public boolean containsOnlyValidTypes() {
        return true;
    }

    @Override public GenericSymbol toGenericSymbol() {
        return this;
    }

    @Override public MathSymbol toMathSymbol() {
        return mathSymbolAlterEgo;
    }

}
