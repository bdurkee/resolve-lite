package org.resolvelite.proving.absyn;

import org.resolvelite.semantics.MTType;

import java.util.*;

public abstract class PExp {

    public final int structureHash;
    public final int valueHash;
    private MTType type, typeValue;

    private Set<String> cachedSymbolNames = null;
    private List<PExp> cachedFunctionApplications = null;
    private Set<PSymbol> cachedQuantifiedVariables = null;

    public PExp(PSymbol.HashDuple hashes, MTType type, MTType typeValue) {
        this(hashes.structureHash, hashes.valueHash, type, typeValue);
    }

    public PExp(int structureHash, int valueHash, MTType type, MTType typeValue) {
        this.type = type;
        this.typeValue = typeValue;
        this.structureHash = structureHash;
        this.valueHash = valueHash;
    }

    @Override public int hashCode() {
        return valueHash;
    }

    public final MTType getMathType() {
        return type;
    }

    public final MTType getMathTypeValue() {
        return typeValue;
    }

    public PExp substitute(PExp current, PExp replacement) {
        Map<PExp, PExp> e = new HashMap<>();
        e.put(current, replacement);
        return substitute(e);
    }

    public void processStringRepresentation(PExpVisitor visitor, Appendable a) {
        /* PExpTextRenderingVisitor renderer = new PExpTextRenderingVisitor(a);
         PExpVisitor finalVisitor = new NestedPExpVisitors(visitor, renderer);

         this.accept(finalVisitor);*/
    }

    public abstract PExp substitute(Map<PExp, PExp> substitutions);

    public abstract boolean containsName(String name);

    public abstract List<PExp> getSubExpressions();

    public abstract boolean isObviouslyTrue();

    public abstract boolean isLiteralTrue();

    public abstract boolean isLiteralFalse();

    public abstract boolean isVariable();

    public abstract boolean isLiteral();

    public abstract boolean isFunction();

    public final List<PExp> splitIntoConjuncts() {
        List<PExp> conjuncts = new ArrayList<>();
        splitIntoConjuncts(conjuncts);
        return conjuncts;
    }

    protected abstract void splitIntoConjuncts(List<PExp> accumulator);

    public abstract PExp flipQuantifiers();

    public final Set<PSymbol> getQuantifiedVariables() {
        if ( cachedQuantifiedVariables == null ) {
            //We're immutable, so only do this once
            cachedQuantifiedVariables =
                    Collections
                            .unmodifiableSet(getQuantifiedVariablesNoCache());
        }
        return cachedQuantifiedVariables;
    }

    public abstract Set<PSymbol> getQuantifiedVariablesNoCache();

    public final List<PExp> getFunctionApplications() {
        if ( cachedFunctionApplications == null ) {
            //We're immutable, so only do this once
            cachedFunctionApplications = getFunctionApplicationsNoCache();
        }
        return cachedFunctionApplications;
    }

    public abstract List<PExp> getFunctionApplicationsNoCache();

    public final Set<String> getSymbolNames() {
        if ( cachedSymbolNames == null ) {
            //We're immutable, so only do this once
            cachedSymbolNames =
                    Collections.unmodifiableSet(getSymbolNamesNoCache());
        }
        return cachedSymbolNames;
    }

    protected abstract Set<String> getSymbolNamesNoCache();

    public static class HashDuple {
        public int structureHash;
        public int valueHash;

        public HashDuple(int structureHash, int valueHash) {
            this.structureHash = structureHash;
            this.valueHash = valueHash;
        }
    }
}
