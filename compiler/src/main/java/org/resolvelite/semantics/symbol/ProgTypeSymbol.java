package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.semantics.MTType;
import org.resolvelite.semantics.programtype.PTInvalid;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.typereasoning.TypeGraph;

public class ProgTypeSymbol extends Symbol {

    private final TypeGraph g;
    protected MTType modelType;
    protected PTType type;
    protected final MathSymbol mathTypeAlterEgo;

    public ProgTypeSymbol(TypeGraph g, String name, PTType progType,
            MTType modelType, ParseTree definingTree, String moduleID) {
        super(name, definingTree, moduleID);
        this.g = g;
        this.type = progType;
        this.modelType = modelType;
        this.mathTypeAlterEgo =
                new MathSymbol(g, name, Quantification.NONE, g.SSET, modelType,
                        definingTree, moduleID);
    }

    public ProgTypeSymbol(TypeGraph g, String name, ParseTree definingTree,
            String moduleID) {
        this(g, name, PTInvalid.getInstance(g), g.INVALID, definingTree,
                moduleID);
    }

    public PTType getProgramType() {
        return type;
    }

    public MTType getModelType() {
        return modelType;
    }

    public void setProgramType(PTType t) {
        this.type = t;
    }

    public void setModelType(MTType t) {
        mathTypeAlterEgo.setTypes(t, null);
        this.modelType = t;
    }

    @Override public MathSymbol toMathSymbol() {
        if ( modelType == null ) {
            throw new IllegalStateException("something is trying to call "
                    + "getMathTypeAlterEgo() prior to having a model type set");
        }
        return mathTypeAlterEgo;
    }

    @Override public String toString() {
        return getName();
    }

    @Override public ProgTypeSymbol toProgTypeSymbol() {
        return this;
    }

    @Override public String getEntryTypeDescription() {
        return "a program type";
    }

    @Override public boolean containsOnlyValidTypes() {
        return mathTypeAlterEgo.containsOnlyValidTypes()
                && !type.getClass().equals(PTInvalid.class);
    }
}
