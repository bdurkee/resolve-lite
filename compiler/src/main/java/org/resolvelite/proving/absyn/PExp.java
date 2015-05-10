package org.resolvelite.proving.absyn;

import org.resolvelite.semantics.MTType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class PExp {

    public final int structureHash;
    public final int valueHash;
    private MTType type, typeValue;

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

    public abstract PExp substitute(Map<PExp, PExp> substitutions);

    public abstract boolean containsName(String name);

    public abstract List<PExp> getSubExpressions();

    public abstract boolean isLiteralTrue();

    public abstract boolean isLiteralFalse();

    public abstract boolean isVariable();

    public abstract boolean isLiteral();

    public abstract boolean isFunction();

    //Todo: I don't think this is terribly necessary. This hierarchy is already
    //immutable and I don't see why we really need a 'copy' anymore. Substitute
    //for instance already makes a copy with the substitutions made.
    public abstract PExp copy();

    public final List<PExp> splitIntoConjuncts() {
        List<PExp> conjuncts = new ArrayList<>();
        splitIntoConjuncts(conjuncts);
        return conjuncts;
    }

    protected abstract void splitIntoConjuncts(List<PExp> accumulator);

    public static class HashDuple {
        public int structureHash;
        public int valueHash;

        public HashDuple(int structureHash, int valueHash) {
            this.structureHash = structureHash;
            this.valueHash = valueHash;
        }
    }
}
