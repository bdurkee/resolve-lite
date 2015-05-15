package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.semantics.MTType;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.semantics.TypeGraph;

public class ProgTypeSymbol extends Symbol {

    protected final MTType modelType;
    protected final PTType type;
    protected final MathSymbol mathTypeAlterEgo;

    public ProgTypeSymbol(TypeGraph g, String name, PTType progType,
            MTType modelType, ParseTree definingTree, String moduleID) {
        super(name, definingTree, moduleID);
        this.type = progType;
        this.modelType = modelType;
        this.mathTypeAlterEgo =
                new MathSymbol(g, name, Quantification.NONE, g.SSET, modelType,
                        definingTree, moduleID);
    }

    public PTType getProgramType() {
        return type;
    }

    public MTType getModelType() {
        return modelType;
    }

    @Override public MathSymbol toMathSymbol() {
        return mathTypeAlterEgo;
    }

    @Override public String toString() {
        return getName();
    }

    @Override public ProgTypeSymbol toProgTypeSymbol() {
        return this;
    }

    @Override public String getSymbolDescription() {
        return "a program type";
    }
}
