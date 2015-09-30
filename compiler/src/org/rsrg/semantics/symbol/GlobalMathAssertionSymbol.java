package org.rsrg.semantics.symbol;

import edu.clemson.resolve.proving.absyn.PExp;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.rsrg.semantics.programtype.PTType;

import java.util.Map;

/**
 * Currently there's no easy way to obtain global (module level) specifications.
 * Consider the VC generator (who needs to grab things like global constraints
 * for all accessible modules) naeeds to manually wrangle with the maps
 * provided by SymbolTable and search the defining elements manually. However,
 * by wrapping these expressions in this symbol, we'll be able to do a
 * multimatch query and get all relevant entries -- from which we can do
 * further processing
 * (or even write a query that does such processing for us)..
 */
public class GlobalMathAssertionSymbol extends Symbol {

    public static enum ClauseType { REQUIRES, CONSTRAINT }
    private final PExp assertion;
    private final ClauseType clauseType;

    public GlobalMathAssertionSymbol(String name, PExp assertion, ClauseType t,
            ParserRuleContext definingTree, String moduleID) {
        super(name, definingTree, moduleID);
        this.assertion = assertion;
        this.clauseType = t;
    }

    public ClauseType getClauseType() {
        return clauseType;
    }

    public PExp getEnclosedExp() {
        return assertion;
    }

    @Override public String getSymbolDescription() {
        return "a module level specification";
    }

    @Override public Symbol instantiateGenerics(
            Map<String, PTType> genericInstantiations,
            FacilitySymbol instantiatingFacility) {
        //Todo: Eventually we'll use the PExp hierarchy to perform generic
        //instantiations on field 'assertion'
        return this;
    }
}
