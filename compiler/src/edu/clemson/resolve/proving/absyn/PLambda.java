package edu.clemson.resolve.proving.absyn;

import edu.clemson.resolve.misc.Utils;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import edu.clemson.resolve.semantics.MathFunctionClssftn;
import edu.clemson.resolve.semantics.MathClssftn;
import edu.clemson.resolve.semantics.MathInvalidClssftn;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/** An anonymous (lambda) function consisting of one or more typed bound variables and a body. */
public class PLambda extends PExp {

    private final List<MathSymbolDeclaration> parameters = new ArrayList<>();
    @NotNull
    private final PExp body;

    public PLambda(@NotNull List<MathSymbolDeclaration> parameters, @NotNull PExp body) {
        this(parameters, body, null, null);
    }

    public PLambda(@NotNull List<MathSymbolDeclaration> parameters,
                   @NotNull PExp body,
                   @Nullable Token vcLocation,
                   @Nullable String vcExplanation) {
        super(body.structureHash * 34, parameterHash(parameters),
                new MathFunctionClssftn(body.getMathClssftn().getTypeGraph(), body.getMathClssftn(),
                        Utils.apply(parameters, p -> p.type)), null,
                vcLocation,
                vcExplanation);
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

    @Override
    public PExp withPrimeMarkAdded() {
        return this;
    }

    @Override
    public void accept(PExpListener v) {
        v.beginPExp(this);
        v.beginPLambda(this);

        v.beginChildren(this);
        body.accept(v);
        v.endChildren(this);

        v.endPLambda(this);
        v.endPExp(this);
    }

    @NotNull
    @Override
    public PExp substitute(@NotNull Map<PExp, PExp> substitutions) {
        PExp result;
        if (substitutions.containsKey(this)) {
            result = substitutions.get(this);
        }
        else {
            result = new PLambda(parameters, body.substitute(substitutions),
                    getVCLocation(), getVCExplanation());
        }
        return result;
    }

    @Override
    public boolean containsName(String name) {
        boolean result = false;
        Iterator<MathSymbolDeclaration> parameterIter = parameters.iterator();
        while (!result && parameterIter.hasNext()) {
            result = parameterIter.next().name.equals(name);
        }
        return result || body.containsName(name);
    }

    @NotNull
    @Override
    public List<PExp> getSubExpressions() {
        return Collections.singletonList(body);
    }

    @Override
    public boolean isObviouslyTrue() {
        return body.isObviouslyTrue();
    }

    public List<PExp> getParametersAsPExps() {
        return Utils.apply(parameters, MathSymbolDeclaration::asPSymbol);
    }

    public List<MathSymbolDeclaration> getParameters() {
        return parameters;
    }

    public PExp getBody() {
        return body;
    }

    @Override
    public boolean isLiteralFalse() {
        return body.isLiteralFalse();
    }

    @NotNull
    @Override
    public String getTopLevelOperationName() {
        return "\\lambda";
    }

    @Override
    protected void splitIntoConjuncts(@NotNull List<PExp> accumulator) {
        accumulator.add(this);
    }

    @Override
    public PExp withVCInfo(@Nullable Token location, @Nullable String explanation) {
        return new PLambda(parameters, body, location, explanation);
    }

    @NotNull
    @Override
    public PExp withIncomingSignsErased() {
        return new PLambda(parameters, body.withIncomingSignsErased(), getVCLocation(), getVCExplanation());
    }

    @NotNull
    @Override
    public PExp withQuantifiersFlipped() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @NotNull
    @Override
    public Set<PSymbol> getIncomingVariablesNoCache() {
        return body.getIncomingVariablesNoCache();
    }

    @Override
    public Set<String> getSymbolNamesNoCache(boolean excludeApplications, boolean excludeLiterals) {
        Set<String> bodyNames = new HashSet<>(body.getSymbolNames(excludeApplications, excludeLiterals));
        //bodyNames.add("lambda"); //not sure why the hell I was adding this...
        return bodyNames;
    }

    @NotNull
    @Override
    public Set<PSymbol> getQuantifiedVariablesNoCache() {
        return body.getQuantifiedVariables();
    }

    @NotNull
    @Override
    public List<PExp> getFunctionApplicationsNoCache() {
        List<PExp> bodyFunctions = new LinkedList<>(body.getFunctionApplications());
        bodyFunctions.add(new PSymbol.PSymbolBuilder("lambda").mathClssfctn(getMathClssftn()).build());
        return bodyFunctions;
    }

    @NotNull
    @Override
    public Set<PSymbol> getFreeVariablesNoCache() {
        return body.getFreeVariables();
    }

    public static class MathSymbolDeclaration {
        public final String name;
        public final MathClssftn type;

        public MathSymbolDeclaration(String name, MathClssftn type) {
            //Todo: Again, I think this should probably be checked before now
            if (name == null) {
                throw new IllegalArgumentException("name==null");
            }
            if (type == null) {
                throw new IllegalArgumentException("type==null");
            }
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public MathClssftn getClssftn() {
            return type;
        }

        public PSymbol asPSymbol() {
            return new PSymbol.PSymbolBuilder(name).mathClssfctn(type).build();
        }

        @Override
        public boolean equals(Object o) {
            boolean result = o instanceof MathSymbolDeclaration;
            if (result) {
                result = this.name.equals(((MathSymbolDeclaration) o).name) &&
                        this.type.equals(((MathSymbolDeclaration) o).type);
            }
            return result;
        }

        @Override
        public String toString() {
            return name + " : " + (type instanceof MathInvalidClssftn ? "Inv" : type);
        }
    }

    //only checks extremely strict equality. No bound variable naming
    //differences allowed.
    @Override
    public boolean equals(Object o) {
        boolean result = (o instanceof PLambda);
        if (result) {
            result = parameters.size() == ((PLambda) o).parameters.size() && body.equals(((PLambda) o).body);
        }
        return result;
    }

    @Override
    public String toString() {
        return "λ " + parameters.get(0) + ", " + body;
    }
}