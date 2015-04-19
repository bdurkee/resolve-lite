package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.semantics.MTType;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.typereasoning.TypeGraph;

public class ProgTypeSymbol extends Symbol {

    private final TypeGraph g;
    protected MTType modelType;
    protected PTType programType;

    public ProgTypeSymbol(TypeGraph g, String name, ParseTree definingTree,
            String moduleID) {
        super(name, definingTree, moduleID);
        this.g = g;
    }

    public PTType getProgramType() {
        return programType;
    }

    public MTType getModelType() {
        return modelType;
    }

    public void setProgramType(PTType t) {
        this.programType = t;
    }

    public void setModelType(MTType t) {
        this.modelType = t;
    }

    /**
     * A program type can masquerade as a math type. This will represent the
     * (non-existent) symbol table entry for the "program type" when viewed as
     * a math type.
     */
    protected MathSymbol getMathTypeAlterEgo() {
        if ( modelType == null ) {
            throw new IllegalStateException("something is trying to call "
                    + "getMathTypeAlterEgo() prior to having a model type set");
        }
        return new MathSymbol(g, getName(), Quantification.NONE,
                getDefiningTree(), g.CLS, modelType, getModuleID());
    }

    @Override public String toString() {
        return getName();
    }

    @Override public String getEntryTypeDescription() {
        return "a program type";
    }
}
