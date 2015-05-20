package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.tree.ParseTree;

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

    /*@Override
    public ProcedureEntry instantiateGenerics(
            Map<String, PTType> genericInstantiations,
            FacilityEntry instantiatingFacility) {

        return new ProcedureEntry(getName(), getDefiningElement(),
                getSourceModuleIdentifier(), myCorrespondingOperation
                .instantiateGenerics(genericInstantiations,
                        instantiatingFacility));
    }*/

    @Override public ProcedureSymbol toProcedureSymbol() {
        return this;
    }
}