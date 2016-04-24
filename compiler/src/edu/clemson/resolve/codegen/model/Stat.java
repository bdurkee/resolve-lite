package edu.clemson.resolve.codegen.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Stat extends OutputModelObject {

    public static class CallStat extends Stat {
        @ModelElement
        public Expr methodParamExp;

        public CallStat(Qualifier qualifier, String name, Expr... args) {
            this(qualifier, name, Arrays.asList(args));
        }

        public CallStat(Expr expr) {
            if (expr instanceof MethodCall.OperationParameterMethodCall ||
                    expr instanceof MethodCall) {
                this.methodParamExp = expr;
            } else {
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

    public static class IfStat extends Stat {

        @ModelElement
        public List<Stat> ifStats = new ArrayList<>();
        @ModelElement
        public List<Stat> elseStats = new ArrayList<>();
        @ModelElement
        public Expr cond;

        public IfStat(Expr cond) {
            this.cond = cond;
        }
    }

    public static class WhileStat extends Stat {
        @ModelElement
        public Expr cond;
        @ModelElement
        public List<Stat> stats = new ArrayList<>();

        public WhileStat(Expr cond) {
            this.cond = cond;
        }
    }
}
