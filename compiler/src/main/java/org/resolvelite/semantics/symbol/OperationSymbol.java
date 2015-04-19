package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.semantics.programtype.PTType;

import java.util.ArrayList;
import java.util.List;

public class OperationSymbol extends Symbol {

    private PTType returnType;
    private final List<ProgParameterSymbol> parameters = new ArrayList<>();

    public OperationSymbol(String name, ParseTree definingTree,
            String moduleID, List<ProgParameterSymbol> params) {
        super(name, definingTree, moduleID);
        this.parameters.addAll(params);
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

    //@Override
    public String getEntryTypeDescription() {
        return "an operation";
    }

}
