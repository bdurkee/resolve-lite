package org.rsrg.semantics.symbol;

import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.semantics.programtype.PTType;

import java.util.Map;

public class ProcedureSymbol extends Symbol {

    private final OperationSymbol correspondingOperation;

    public ProcedureSymbol(String name, ParseTree definingTree,
            String moduleID, OperationSymbol correspondingOperation) {
        super(name, definingTree, moduleID);

        this.correspondingOperation = correspondingOperation;
    }

    public OperationSymbol getCorrespondingOperation() {
        return correspondingOperation;
    }

    @Override public String getSymbolDescription() {
        return "a procedure";
    }

    @Override public ProcedureSymbol instantiateGenerics(
            Map<String, PTType> genericInstantiations,
            FacilitySymbol instantiatingFacility) {

        return new ProcedureSymbol(getName(), getDefiningTree(), getModuleID(),
                correspondingOperation.instantiateGenerics(
                        genericInstantiations, instantiatingFacility));
    }

    @Override public ProcedureSymbol toProcedureSymbol() {
        return this;
    }
}