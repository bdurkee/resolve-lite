package org.resolvelite.semantics.symbol;

/**
 * Wraps a normal {@link FunctionSymbol} for easily identifying functions that
 * happen to be functioning as formal parameters.
 */
public class OperationParameterSymbol extends BaseSymbol
        implements ModuleParameterSymbol {

    public OperationParameterSymbol(FunctionSymbol f) {
        super(f.getName(), f.getRootModuleID());
    }
}
