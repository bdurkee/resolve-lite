package org.resolvelite.proving.absyn;

import edu.emory.mathcs.backport.java.util.Collections;
import org.resolvelite.misc.Utils;
import org.resolvelite.semantics.MTType;
import org.resolvelite.semantics.programtype.PTType;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PDot extends PExp {

    private final List<PSymbol> segs = new ArrayList<>();

    public PDot(MTType type, MTType typeValue, PSymbol... segs) {
        this(Arrays.asList(segs), type, typeValue, null, null);
    }

    public PDot(List<PSymbol> segs, MTType type, MTType typeValue) {
        this(segs, type, typeValue, null, null);
    }

    public PDot(List<PSymbol> segs, MTType type, MTType typeValue,
            PTType progType, PTType progTypeValue) {
        super(PSymbol.calculateHashes(segs), type, typeValue, progType,
                progTypeValue);
        this.segs.addAll(segs);
    }

    @Override public void accept(PExpVisitor v) {
        v.beginPExp(this);
        v.beginPDot(this);

        v.beginChildren(this);
        for (PSymbol segment : segs) {
            segment.accept(v);
        }
        v.endChildren(this);

        v.endPDot(this);
        v.endPExp(this);
    }

    @Override public PExp substitute(Map<PExp, PExp> substitutions) {
        PExp result = substitutions.get(this);

        if ( result == null ) {
            List<PSymbol> newSegments = new ArrayList<>();

            for (PSymbol p : segs) {
                PExp x1 = p.substitute(substitutions);

                if ( x1 instanceof PDot ) { //flatten the dot exp
                    PDot x1AsPDot = (PDot) x1;
                    for (PSymbol s : x1AsPDot.getSegments()) {
                        newSegments.add(s);
                    }
                }
                else {
                    newSegments.add((PSymbol) x1);
                }
            }
            return new PDot(newSegments, getMathType(), getMathTypeValue(),
                    getProgType(), getProgTypeValue());
        }
        return result;
    }

    public List<PSymbol> getSegments() {
        return segs;
    }

    @Override public boolean containsName(String name) {
        for (PExp s : segs) {
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
                .map(PExp::withIncomingSignsErased).map(s -> (PSymbol) s)
                .collect(Collectors.toList());
        return new PDot(newSegs, getMathType(), getMathTypeValue(),
                getProgType(), getProgTypeValue());
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

    @Override public boolean equals(Object o) {
        boolean result = (o instanceof PDot);
        if ( result ) {
            List<PSymbol> oSegs = ((PDot) o).getSegments();
            if ( oSegs.size() != this.getSegments().size() ) {
                result = false;
            }
            Iterator<PSymbol> oSegsIter = ((PDot) o).getSegments().iterator();
            Iterator<PSymbol> thisSegsIter = this.getSegments().iterator();

            while (result && oSegsIter.hasNext() && thisSegsIter.hasNext()) {
                result = oSegsIter.next().equals(thisSegsIter.next());
            }
        }
        return result;
    }

    @Override public String toString() {
        return Utils.join(segs, ".");
    }
}
