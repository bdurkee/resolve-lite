package edu.clemson.resolve.codegen.model;

import java.util.Arrays;
import java.util.List;

public class CallStat extends Stat {
    @ModelElement public Expr methodParamExp;

    public CallStat(Qualifier qualifier, String name, Expr... args) {
        this(qualifier, name, Arrays.asList(args));
    }

    public CallStat(Expr expr) {
        if ( expr instanceof MethodCall.OperationParameterMethodCall ||
                expr instanceof MethodCall) {
            this.methodParamExp = expr;
        }
        else {
            throw new IllegalArgumentException("expr doesn't describe a call");
        }
    }

    public CallStat(MethodCall exprCall) {
        this(exprCall.q, exprCall.name, exprCall.args);
    }

    public CallStat(Qualifier qualifier, String name, List<Expr> args) {
        this.methodParamExp = new MethodCall(qualifier, name, args);
    }

    public CallStat(MethodCall.OperationParameterMethodCall exprCall) {
        this.methodParamExp = exprCall;
    }
}