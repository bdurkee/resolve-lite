package edu.clemson.resolve.codegen.model;

import edu.clemson.resolve.semantics.programtype.ProgVoidType;
import edu.clemson.resolve.semantics.symbol.OperationSymbol;
import edu.clemson.resolve.semantics.symbol.ProgParameterSymbol;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an anonymous class that wraps the invocation of an operation
 * that we wish to pass into another module (via a facility, etc).
 */
public class AnonOpParameterClassInstance extends Expr {
    public String name;
    public boolean hasReturn = false;
    public List<ProgParameterSymbol> params = new ArrayList<>();
    @ModelElement
    public Qualifier q;

    public AnonOpParameterClassInstance(Qualifier wrappedFunctionQualifier,
                                        OperationSymbol f) {
        this.name = f.getName();
        this.q = wrappedFunctionQualifier;
        this.hasReturn = !(f.getReturnType() instanceof ProgVoidType);
        this.params = f.getParameters();
    }
}