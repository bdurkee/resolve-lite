package edu.clemson.resolve.codegen.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This class is different (and neccessary) even though we already have an
 * {@link CallStat} class. This is intended to represent calls appearing within
 * the context of an arbitrary expression and are not required
 * Wheras
 */
public class MethodCall extends Expr {
    public String name;
    @ModelElement public List<Expr> args = new ArrayList<>();
    @ModelElement public Qualifier q;

    public MethodCall(Qualifier qualifier, String name, Expr ... args) {
        this(qualifier, name, Arrays.asList(args));
    }

    public MethodCall(Qualifier qualifier, String name, List<Expr> args) {
        this.name = name;
        this.q = qualifier;
        this.args.addAll(args);
    }

    /**
     * Two special constructors used to create create getter calls for variables
     * referencing things like module level generics and formal params
     */
    public MethodCall(VarNameRef nameRef) {
        this(nameRef.q, "get" + nameRef.name, Collections.emptyList());
    }

    public MethodCall(TypeInit genericTypeInit) {
        this(genericTypeInit.q, "get" + genericTypeInit.typeName, Collections
                .emptyList());
    }

    //i.e.: ((OperationParameter) Read_Element).op(Next);
    public static class OperationParameterMethodCall extends Expr {
        public String name;
        @ModelElement public List<Expr> args = new ArrayList<>();
        public OperationParameterMethodCall(String name, List<Expr> args) {
            this.name = name;
            this.args.addAll(args);
        }
    }
}