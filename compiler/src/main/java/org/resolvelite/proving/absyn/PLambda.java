package org.resolvelite.proving.absyn;

import org.resolvelite.semantics.MTFunction;
import org.resolvelite.semantics.MTType;

import java.util.*;
import java.util.stream.Collectors;

public class PLambda extends PExp {

    private final List<Parameter> parameters = new ArrayList<>();
    private final PExp body;

    public PLambda(List<Parameter> parameters, PExp body) {
        super(body.structureHash * 34, parameterHash(parameters),
                new MTFunction.MTFunctionBuilder(body.getMathType()
                        .getTypeGraph(), body.getMathType())
                        .paramTypes(parameters.stream()
                                .map(p ->p.type).collect(Collectors.toList()))
                        .build(), null);
        this.parameters.addAll(parameters);
        this.body = body;
    }

    private static int parameterHash(Iterable<Parameter> parameters) {
        int hash = 0;
        for (Parameter p : parameters) {
            hash += p.name.hashCode() * 27 + p.type.hashCode();
            hash *= 49;
        }
        return hash;
    }

    @Override public void accept(PExpVisitor v) {
        v.beginPExp(this);
        v.beginPLambda(this);

        v.beginChildren(this);
        body.accept(v);
        v.endChildren(this);

        v.endPLambda(this);
        v.endPExp(this);
    }

    @Override
    public PExp substitute(Map<PExp, PExp> substitutions) {
        PExp retval;
        if (substitutions.containsKey(this)) {
            retval = substitutions.get(this);
        }
        else {
            retval = new PLambda(parameters, body.substitute(substitutions));
        }
        return retval;
    }

    @Override public boolean containsName(String name) {
        boolean result = false;
        Iterator<Parameter> parameterIter = parameters.iterator();
        while (!result && parameterIter.hasNext()) {
            result = parameterIter.next().name.equals(name);
        }
        return result || body.containsName(name);
    }

    @Override public List<PExp> getSubExpressions() {
        return Collections.singletonList(body);
    }

    @Override public boolean isObviouslyTrue() {
        return body.isObviouslyTrue();
    }

    @Override public boolean isLiteralTrue() {
        return body.isObviouslyTrue();
    }

    @Override public boolean isLiteralFalse() {
        return body.isLiteralFalse();
    }

    @Override
    public boolean isVariable() {
        return false;
    }

    @Override
    public boolean isLiteral() {
        return false;
    }

    @Override
    public boolean isFunction() {
        return false;
    }

    @Override protected void splitIntoConjuncts(List<PExp> accumulator) {
        accumulator.add(this);
    }

    @Override
    public PExp withIncomingSignsErased() {
        return null;
    }

    @Override
    public PExp flipQuantifiers() {
        return null;
    }

    @Override
    public Set<PSymbol> getIncomingVariablesNoCache() {
        return null;
    }

    @Override
    public Set<PSymbol> getQuantifiedVariablesNoCache() {
        return null;
    }

    @Override
    public List<PExp> getFunctionApplicationsNoCache() {
        return null;
    }

    @Override
    protected Set<String> getSymbolNamesNoCache() {
        return null;
    }

    public static class Parameter {
        public final String name;
        public final MTType type;

        public Parameter(String name, MTType type) {
            if (name == null) {
                throw new IllegalArgumentException("name==null");
            }
            if (type == null) {
                throw new IllegalArgumentException("type==null");
            }
            this.name = name;
            this.type = type;
        }

        @Override public String toString() {
            return name + ":" + type;
        }
    }
}
