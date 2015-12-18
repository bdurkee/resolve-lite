package org.rsrg.semantics.symbol;

import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.ModuleIdentifier;
import org.rsrg.semantics.programtype.PTType;

import java.util.Map;

public class ProcedureSymbol extends Symbol {

    @NotNull private final OperationSymbol correspondingOperation;

    public ProcedureSymbol(@NotNull String name,
                           @Nullable ParserRuleContext definingTree,
                           @NotNull ModuleIdentifier moduleIdentifier,
                           @NotNull OperationSymbol correspondingOperation) {
        super(name, definingTree, moduleIdentifier);
        this.correspondingOperation = correspondingOperation;
    }

    @NotNull public OperationSymbol getCorrespondingOperation() {
        return correspondingOperation;
    }

    @NotNull @Override public String getSymbolDescription() {
        return "a procedure";
    }

    @NotNull @Override public ProcedureSymbol instantiateGenerics(
            @NotNull Map<String, PTType> genericInstantiations,
            @Nullable FacilitySymbol instantiatingFacility) {

        return new ProcedureSymbol(getName(), getDefiningTree(), getModuleIdentifier(),
                correspondingOperation.instantiateGenerics(
                        genericInstantiations, instantiatingFacility));
    }

    @NotNull @Override public ProcedureSymbol toProcedureSymbol() {
        return this;
    }
}