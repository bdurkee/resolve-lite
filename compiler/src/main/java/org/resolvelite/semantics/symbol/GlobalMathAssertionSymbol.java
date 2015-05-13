package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.parsing.ResolveParser;

/**
 * Currently there's no easy way to obtain global (module level) specifications.
 * Consider the VC generator who needs to grab global constraints for all
 * accessible modules. Right now, you need to manually wrangle with the maps
 * provided by SymbolTable and search the defining elements manually. However,
 * by wrapping these expressions in this symbol, we'll be able to do a
 * multimatch
 * query and get all relevant entries -- from which we can do further processing
 * (or even write a query that does such processing for us)..
 */
public class GlobalMathAssertionSymbol extends Symbol {

    private ResolveParser.MathAssertionExpContext assertion;

    //No need to pass the specific mathExpContext into this. What we actually
    //want is already being passed in the form of 'definingTree': It will be
    //ConstraintClauseContext, RequiresClauseContext, etc, this will allow us
    //to filter these entries by the type of clause using something like:
    //".getDefiningTree() instanceof ResolveParser.RequiresClauseContext"
    public GlobalMathAssertionSymbol(String name,
            ResolveParser.MathAssertionExpContext assertion,
            ParseTree definingTree, String moduleID) {
        super(name, definingTree, moduleID);
        this.assertion = assertion;
    }

    public ResolveParser.MathAssertionExpContext getAssertion() {
        return assertion;
    }

    @Override public String getSymbolDescription() {
        return "a module level specification";
    }
}
