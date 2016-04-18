package edu.clemson.resolve.codegen.model;

import org.rsrg.semantics.programtype.ProgVoidType;
import org.rsrg.semantics.symbol.OperationSymbol;
import org.rsrg.semantics.symbol.ProgParameterSymbol;

import java.util.ArrayList;
import java.util.List;

/** Represents an anonymous class that wraps the invocation of an operation
 *  that we wish to pass into another module (via a facility, etc).
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