package org.resolvelite.codegen.model;

import org.resolvelite.semantics.symbol.FunctionSymbol;
import org.resolvelite.semantics.symbol.ParameterSymbol;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an anonymous class that wraps the invocation of an operation
 * that we wish to another module.
 */
public class AnonymousOpParameterInstance extends Expr {

    public String name;
    public boolean hasReturn = false;
    public List<ParameterSymbol> params = new ArrayList<>();

    public AnonymousOpParameterInstance(FunctionSymbol f) {
        this.name = f.getName();
        this.hasReturn = !f.getType().getName().equals("Void");
        this.params = f.getSymbolsOfType(ParameterSymbol.class);
    }
}
