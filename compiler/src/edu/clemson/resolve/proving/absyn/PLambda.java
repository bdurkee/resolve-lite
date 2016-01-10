package edu.clemson.resolve.proving.absyn;

import edu.clemson.resolve.misc.Utils;
import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.MTFunction;
import org.rsrg.semantics.MTInvalid;
import org.rsrg.semantics.MTType;

import java.util.*;
import java.util.stream.Collectors;

/** An anonymous (lambda) function consisting of one or more typed bound
 *  variables and a body.
 */
public class PLambda extends PExp {

    private final List<MathSymbolDeclaration> parameters = new ArrayList<>();
    @NotNull private final PExp body;

    public PLambda(@NotNull List<MathSymbolDeclaration> parameters, @NotNull PExp body) {
        super(body.structureHash * 34, parameterHash(parameters),
                new MTFunction.MTFunctionBuilder(body.getMathType()
                        .getTypeGraph(), body.getMathType())
                        .paramTypes(parameters.stream()
                                .map(p ->p.type).collect(Collectors.toList()))
                        .build(), null);
        this.parameters.addAll(parameters);
        this.body = body;
    }

    private static int parameterHash(Iterable<MathSymbolDeclaration> parameters) {
        int hash = 0;
        for (MathSymbolDeclaration p : parameters) {
            hash += p.name.hashCode() * 27 + p.type.hashCode();
            hash *= 49;
        }
        return hash;
    }

    @Override public void accept(PExpListener v) {
        v.beginPExp(this);
        v.beginPLambda(this);

        v.beginChildren(this);
        body.accept(v);
        v.endChildren(this);

        v.endPLambda(this);
        v.endPExp(this);
    }

    @NotNull @Override public PExp substitute(@NotNull Map<PExp, PExp> substitutions) {
        PExp result;
        if ( substitutions.containsKey(this) ) {
            result = substitutions.get(this);
        }
        else {
            result = new PLambda(parameters, body.substitute(substitutions));
        }
        return result;
    }

    @Override public boolean containsName(String name) {
        boolean result = false;
        Iterator<MathSymbolDeclaration> parameterIter = parameters.iterator();
        while (!result && parameterIter.hasNext()) {
            result = parameterIter.next().name.equals(name);
        }
        return result || body.containsName(name);
    }

    @NotNull @Override public List<PExp> getSubExpressions() {
        return Collections.singletonList(body);
    }

    @Override public boolean isObviouslyTrue() {
        return body.isObviouslyTrue();
    }

    public List<PExp> getParametersAsPExps() {
        return parameters.stream().map(p -> new PSymbol.PSymbolBuilder(p.name)
                .mathType(p.type).build()).collect(Collectors.toList());
    }

    public List<MathSymbolDeclaration> getParameters() {
        return parameters;
    }

    public PExp getBody() {
        return body;
    }

    @Override public boolean isLiteralFalse() {
        return body.isLiteralFalse();
    }

    @NotNull @Override public String getCanonicalName() {
        return "\\lambda";
    }

    @Override protected void splitIntoConjuncts(@NotNull List<PExp> accumulator) {
        accumulator.add(this);
    }

    @NotNull @Override public PExp withIncomingSignsErased() {
        return new PLambda(parameters, body.withIncomingSignsErased());
    }

    @NotNull @Override public PExp withQuantifiersFlipped() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @NotNull @Override public Set<PSymbol> getIncomingVariablesNoCache() {
        return body.getIncomingVariablesNoCache();
    }

    @Override public Set<String> getSymbolNamesNoCache(boolean excludeApplications, boolean excludeLiterals) {
        Set<String> bodyNames =
                new HashSet<>(body.getSymbolNames(excludeApplications, excludeLiterals));
        //bodyNames.add("lambda"); //not sure why the hell I was adding this...
        return bodyNames;
    }

    @NotNull @Override public Set<PSymbol> getQuantifiedVariablesNoCache() {
        return body.getQuantifiedVariables();
    }

    @NotNull @Override public List<PExp> getFunctionApplicationsNoCache() {
        List<PExp> bodyFunctions =
                new LinkedList<>(body.getFunctionApplications());
        bodyFunctions.add(new PSymbol.PSymbolBuilder("lambda").mathType(
                getMathType()).build());
        return bodyFunctions;
    }

    public static class MathSymbolDeclaration {
        public final String name;
        public final MTType type;

        public MathSymbolDeclaration(String name, MTType type) {
            //Todo: Again, I think this should probably be checked before now
            if ( name == null ) {
                throw new IllegalArgumentException("name==null");
            }
            if ( type == null ) {
                throw new IllegalArgumentException("type==null");
            }
            this.name = name;
            this.type = type;
        }

        @Override public boolean equals(Object o) {
            boolean result = o instanceof MathSymbolDeclaration;
            if ( result ) {
                result = this.name.equals(((MathSymbolDeclaration)o).name) &&
                        this.type.equals(((MathSymbolDeclaration)o).type);
            }
            return result;
        }

        @Override public String toString() {
            return name + ":" + (type instanceof MTInvalid ? "Inv" : type);
        }
    }

    //only checks extremely strict equality. No bound variable naming
    //differences allowed.
    @Override public boolean equals(Object o) {
        boolean result = (o instanceof PLambda);
        if ( result ) {
            result = parameters.size() == ((PLambda)o).parameters.size() &&
                    body.equals(((PLambda)o).body);
        }
        return result;
    }

    @Override public String toString() {
        return "lambda(" + Utils.join(parameters, ", ") + ").(" + body + ")";
    }
}