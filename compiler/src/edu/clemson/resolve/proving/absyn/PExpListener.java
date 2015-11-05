package edu.clemson.resolve.proving.absyn;

public abstract class PExpListener {

    // General
    public void beginPExp(PExp p) {}

    public void endPExp(PExp p) {}

    public void beginChildren(PExp p) {}

    public void endChildren(PExp p) {}

    // PApply
    public void beginPApply(PApply p) {}

    public void fencepostPApply(PApply p) {}

    public void endPApply(PApply p) {}

        // PrefixPApply
        public void beginPrefixPApply(PApply p) {}

        public void fencepostPrefixPApply(PApply p) {}

        public void endPrefixPApply(PApply p) {}

        // InfixPApply
        public void beginInfixPApply(PApply p) {}

        public void fencepostInfixPApply(PApply p) {}

        public void endInfixPApply(PApply p) {}

        // OutfixPApply
        public void beginOutfixPApply(PApply p) {}

        public void fencepostOutfixPApply(PApply p) {}

        public void endOutfixPApply(PApply p) {}

        // PostfixPApply
        public void beginPostfixPApply(PApply p) {}

        public void fencepostPostfixPApply(PApply p) {}

        public void endPostfixPApply(PApply p) {}

    //PSymbol
    public void beginPSymbol(PSymbol p) {}

    public void fencepostPSymbol(PSymbol p) {}

    public void endPSymbol(PSymbol p) {}

    //PAlternatives
    public void beginPAlternatives(PAlternatives p) {}

    public void fencepostPAlternatives(PAlternatives p) {}

    public void endPAlternatives(PAlternatives p) {}

    //PLambda
    public void beginPLambda(PLambda p) {}

    public void endPLambda(PLambda p) {}

    //PSet
    public void beginPSet(PSet p) {}

    public void fencepostPSet(PSet p) {}

    public void endPSet(PSet p) {}


}
