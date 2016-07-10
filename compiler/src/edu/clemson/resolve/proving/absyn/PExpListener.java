package edu.clemson.resolve.proving.absyn;

import org.jetbrains.annotations.NotNull;

/**
 * This class provides a general, empty implementation of a listener for the {@link PExp} hierarchy. This
 * class can be extended to create a listener which only needs to handle a subset of the available methods.
 */
public abstract class PExpListener {

    // General
    public void beginPExp(@NotNull PExp p) {
    }

    public void endPExp(@NotNull PExp p) {
    }

    public void beginChildren(@NotNull PExp p) {
    }

    public void endChildren(@NotNull PExp p) {
    }

    // PApply
    public void beginPApply(@NotNull PApply p) {
    }

    public void fencepostPApply(@NotNull PApply p) {
    }

    public void endPApply(@NotNull PApply p) {
    }

    // PrefixPApply
    public void beginPrefixPApply(@NotNull PApply p) {
    }

    public void fencepostPrefixPApply(@NotNull PApply p) {
    }

    public void endPrefixPApply(@NotNull PApply p) {
    }

    // InfixPApply
    public void beginInfixPApply(@NotNull PApply p) {
    }

    public void fencepostInfixPApply(@NotNull PApply p) {
    }

    public void endInfixPApply(@NotNull PApply p) {
    }

    // OutfixPApply
    public void beginOutfixPApply(@NotNull PApply p) {
    }

    public void fencepostOutfixPApply(@NotNull PApply p) {
    }

    public void endOutfixPApply(@NotNull PApply p) {
    }

    // PostfixPApply
    public void beginPostfixPApply(@NotNull PApply p) {
    }

    public void fencepostPostfixPApply(@NotNull PApply p) {
    }

    public void endPostfixPApply(@NotNull PApply p) {
    }

    //PSymbol
    public void beginPSymbol(@NotNull PSymbol p) {
    }

    public void fencepostPSymbol(@NotNull PSymbol p) {
    }

    public void endPSymbol(@NotNull PSymbol p) {
    }

    //PSelector

    public void beginPSelector(@NotNull PSelector p) {
    }

    public void fencepostPSelector(@NotNull PSelector p) {
    }

    public void endPSelector(@NotNull PSelector p) {
    }

    //PAlternatives
    public void beginPAlternatives(@NotNull PAlternatives p) {
    }

    public void fencepostPAlternatives(@NotNull PAlternatives p) {
    }

    public void endPAlternatives(@NotNull PAlternatives p) {
    }

    //PLambda
    public void beginPLambda(@NotNull PLambda p) {
    }

    public void endPLambda(@NotNull PLambda p) {
    }

    //PSet
    public void beginPSet(@NotNull PSet p) {
    }

    public void fencepostPSet(@NotNull PSet p) {
    }

    public void endPSet(@NotNull PSet p) {
    }
}
