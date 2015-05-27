package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.semantics.programtype.PTType;

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
 * 
 * 
 */
//Todo: Maybe these should actually be added in a phase following compute types
//so we can just put the already typed PExps in here AND have hte ability to
//instantiate generics
public class GlobalMathAssertionSymbol extends Symbol {

    private PExp assertion;

    public GlobalMathAssertionSymbol(String name, PExp assertion,
            ParseTree definingTree, String moduleID) {
        super(name, definingTree, moduleID);
        this.assertion = assertion;
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
