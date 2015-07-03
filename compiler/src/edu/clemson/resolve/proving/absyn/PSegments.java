package edu.clemson.resolve.proving.absyn;

import edu.clemson.resolve.misc.Utils;

import java.util.*;
import java.util.stream.Collectors;

public class PSegments extends PExp {

    private final List<PSymbol> segs = new ArrayList<>();
    private final boolean incoming;

    public PSegments(PSymbol... segs) {
        this(Arrays.asList(segs));
    }

    public PSegments(List<PSymbol> segs) {
        this(segs, segs.get(segs.size() - 1), false);
    }

    public PSegments(List<PSymbol> segs, boolean incoming) {
        this(segs, segs.get(segs.size() - 1), incoming);
    }

    public PSegments(List<PSymbol> segs, PSymbol lastSegment, boolean incoming) {
        super(PSymbol.calculateHashes(segs), lastSegment.getMathType(),
                lastSegment.getMathTypeValue());
        this.segs.addAll(segs);
        this.incoming = incoming;
    }

    @Override public void accept(PExpListener v) {
        v.beginPExp(this);
        v.beginPSegments(this);

        v.beginChildren(this);
        for (PSymbol segment : segs) {
            segment.accept(v);
        }
        v.endChildren(this);

        v.endPSegments(this);
        v.endPExp(this);
    }

    @Override public PExp substitute(Map<PExp, PExp> substitutions) {
        
        PExp result = substitutions.get(this);

        if ( result == null ) {
            List<PSymbol> newSegments = new ArrayList<>();

            for (PSymbol p : segs) {
                PExp x1 = p.substitute(substitutions);

                if ( x1 instanceof PSegments ) { //flatten the dot exp
                    PSegments x1AsPDot = (PSegments) x1;
                    for (PSymbol s : x1AsPDot.getSegments()) {
                        newSegments.add(s);
                    }
                }
                else {
                    newSegments.add((PSymbol) x1);
                }
            }
            return new PSegments(newSegments);
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

    @Override public boolean isLiteralFalse() {
        return false;
    }

    @Override public boolean isVariable() {
        return false;
    }

    @Override public boolean isLiteral() {
        return false;
    }

    //Todo: Move this upto PExp.
    public boolean isIncoming() {
        return incoming;
    }

    @Override public boolean isFunction() {
        return false;
    }

    @Override protected void splitIntoConjuncts(List<PExp> accumulator) {}

    @Override public PExp withIncomingSignsErased() {
        List<PSymbol> newSegs = segs.stream()
                .map(PExp::withIncomingSignsErased).map(s -> (PSymbol) s)
                .collect(Collectors.toList());
        return new PSegments(newSegs);
    }

    @Override public PExp withQuantifiersFlipped() {
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
        boolean result = (o instanceof PSegments);
        if ( result ) {
            List<PSymbol> oSegs = ((PSegments) o).getSegments();
            if ( oSegs.size() != this.getSegments().size() ) {
                result = false;
            }
            Iterator<PSymbol> oSegsIter =
                    ((PSegments) o).getSegments().iterator();
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