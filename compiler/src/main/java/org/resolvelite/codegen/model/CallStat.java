package org.resolvelite.codegen.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CallStat extends Stat {

    @ModelElement public List<Expr> args = new ArrayList<>();
    @ModelElement public Qualifier q;
    public String name;

    public CallStat(Qualifier qualifier, String name, Expr... args) {
        this(qualifier, name, Arrays.asList(args));
    }

    public CallStat(Qualifier qualifier, String name, List<Expr> args) {
        this.q = qualifier;
        this.name = name;
        this.args.addAll(args);
    }

    public CallStat(MethodCall exprCall) {
        this(exprCall.q, exprCall.name, exprCall.args);
    }
}