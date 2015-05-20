package org.resolvelite.proving.absyn;

import edu.emory.mathcs.backport.java.util.Collections;
import org.resolvelite.misc.Utils;
import org.resolvelite.semantics.MTType;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PDot extends PExp {

    private final List<PSymbol> segs = new ArrayList<>();

    public PDot(MTType type, MTType typeValue, PSymbol... segs) {
        this(Arrays.asList(segs), type, typeValue);
    }

    public PDot(List<PSymbol> segs, MTType type, MTType typeValue) {
        super(PSymbol.calculateHashes(segs), type, typeValue);
        this.segs.addAll(segs);
    }

    @Override public PExp substitute(Map<PExp, PExp> substitutions) {
        List<PSymbol> segz = segs.stream().map(s -> substitute(substitutions))
                .map(s -> (PSymbol)s)
                .collect(Collectors.toList());
        return new PDot(segz, getMathType(), getMathTypeValue());
    }

    @Override public boolean containsName(String name) {
        for (PSymbol s : segs) {
            if ( s.containsName(name) ) return true;
        }
        return false;
    }

    @Override public List<? extends PExp> getSubExpressions() {
        return segs;
    }

    @Override public boolean isObviouslyTrue() {
        return false;
    }

    @Override public boolean isLiteralTrue() {
        return false;
    }

    @Override public boolean isLiteralFalse() {
        return false;
    }

    @Override public boolean isVariable() {
        return false;
    }

    @Override public boolean isLiteral() {
        return false;
    }

    @Override public boolean isFunction() {
        return false;
    }

    @Override protected void splitIntoConjuncts(List<PExp> accumulator) {

    }

    @Override public PExp withIncomingSignsErased() {
        List<PSymbol> newSegs = segs.stream()
                .map(PSymbol::withIncomingSignsErased).map(s -> (PSymbol) s)
                .collect(Collectors.toList());
        return new PDot(newSegs, getMathType(), getMathTypeValue());
    }

    @Override public PExp flipQuantifiers() {
        return this;
    }

    @Override public Set<PSymbol> getIncomingVariablesNoCache() {
        return new HashSet<>();
    }

    @Override public Set<PSymbol> getQuantifiedVariablesNoCache() {
        return new HashSet<>();
    }

    @Override public List<PExp> getFunctionApplicationsNoCache() {
        return new ArrayList<>();
    }

    @Override protected Set<String> getSymbolNamesNoCache() {
        return new HashSet<>();
    }

    @Override public String toString() {
        return Utils.join(segs, ".");
    }
}
