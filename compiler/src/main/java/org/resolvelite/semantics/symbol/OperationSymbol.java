package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.semantics.programtype.PTInvalid;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.ArrayList;
import java.util.List;

public class OperationSymbol extends Symbol {

    private PTType returnType;
    private final List<ProgParameterSymbol> parameters = new ArrayList<>();

    public OperationSymbol(TypeGraph g, String name, ParseTree definingTree,
            PTType type, String moduleID, List<ProgParameterSymbol> params) {
        super(name, definingTree, moduleID);
        this.parameters.addAll(params);
        this.returnType = type;
    }

    public List<ProgParameterSymbol> getParameters() {
        return parameters;
    }

    public PTType getReturnType() {
        return returnType;
    }

    public void setReturnType(PTType t) {
        this.returnType = t;
    }

    @Override public OperationSymbol toOperationSymbol() {
        return this;
    }

    @Override public String getEntryTypeDescription() {
        return "an operation";
    }

    @Override public String toString() {
        return getName() + ":" + parameters;
    }

}
