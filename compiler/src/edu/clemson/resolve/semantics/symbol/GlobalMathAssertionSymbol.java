package edu.clemson.resolve.semantics.symbol;

import edu.clemson.resolve.proving.absyn.PExp;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import edu.clemson.resolve.semantics.ModuleIdentifier;
import edu.clemson.resolve.semantics.programtype.ProgType;

import java.util.Map;

/**
 * Currently there's no easy way to obtain global (module level) specifications.
 * Consider the VC generator (who needs to grab things like global constraints
 * for all accessible modules) naeeds to manually wrangle with the maps
 * provided by MathSymbolTable and search the defining elements manually. However,
 * by wrapping these expressions in this symbol, we'll be able to do a
 * multimatch query and get all relevant entries -- from which we can do
 * further processing
 * (or even write a query that does such processing for us)..
 */
public class GlobalMathAssertionSymbol extends Symbol {

    public static enum ClauseType {REQUIRES, CONSTRAINT}

    @NotNull
    private final PExp assertion;
    @NotNull
    private final ClauseType clauseType;

    public GlobalMathAssertionSymbol(@NotNull String name,
                                     @NotNull PExp assertion,
                                     @NotNull ClauseType t,
                                     @Nullable ParserRuleContext definingTree,
                                     @NotNull ModuleIdentifier moduleIdentifier) {
        super(name, definingTree, moduleIdentifier);
        this.assertion = assertion;
        this.clauseType = t;
    }

    @NotNull
    public ClauseType getClauseType() {
        return clauseType;
    }

    @NotNull
    public PExp getEnclosedExp() {
        return assertion;
    }

    @NotNull
    @Override
    public String getSymbolDescription() {
        return "a module level specification";
    }

    @NotNull
    @Override
    public Symbol instantiateGenerics(
            @NotNull Map<String, ProgType> genericInstantiations,
            @Nullable FacilitySymbol instantiatingFacility) {
        //Todo: Eventually we'll use the PExp hierarchy to perform generic
        //instantiations on field 'assertion'
        return this;
    }
}
