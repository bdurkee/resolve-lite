package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.semantics.programtype.PTInvalid;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.ArrayList;
import java.util.List;

public class OperationSymbol extends Symbol {

    private final PTType returnType;
    private final List<ProgParameterSymbol> parameters = new ArrayList<>();
    private final boolean moduleParameter;

    private final ResolveParser.RequiresClauseContext requires;
    private final ResolveParser.EnsuresClauseContext ensures;

    public OperationSymbol(TypeGraph g, String name, ParseTree definingTree,
            ResolveParser.RequiresClauseContext requires,
            ResolveParser.EnsuresClauseContext ensures, PTType type,
            String moduleID, List<ProgParameterSymbol> params,
            boolean moduleParameter) {
        super(name, definingTree, moduleID);
        this.parameters.addAll(params);
        this.returnType = type;
        this.moduleParameter = moduleParameter;
        this.requires = requires;
        this.ensures = ensures;
    }

    public ResolveParser.RequiresClauseContext getRequires() {
        return requires;
    }

    public ResolveParser.EnsuresClauseContext getEnsures() {
        return ensures;
    }

    public boolean isModuleParameter() {
        return moduleParameter;
    }

    public List<ProgParameterSymbol> getParameters() {
        return parameters;
    }

    public PTType getReturnType() {
        return returnType;
    }

    @Override public OperationSymbol toOperationSymbol() {
        return this;
    }

    @Override public String getSymbolDescription() {
        return "an operation";
    }

    @Override public String toString() {
        return getName() + ":" + parameters;
    }

}
