package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.ParserRuleContext;
import org.resolvelite.semantics.DuplicateSymbolException;
import org.resolvelite.semantics.Scope;
import org.resolvelite.semantics.SymbolTable;
import org.resolvelite.semantics.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates operation declarations, procedures, and facilityoperations.
 */
public class FunctionSymbol extends SymbolWithScope implements TypedSymbol {
    protected ParserRuleContext tree;
    protected Type retType;

    public FunctionSymbol(String name, ParserRuleContext tree,
            SymbolTable scopeRepo, String rootModuleID) {
        super(name, scopeRepo, rootModuleID);
        this.tree = tree;
    }

    @Override public Type getType() {
        return retType;
    }

    @Override public void setType(Type type) {
        retType = type;
    }

    @Override public Symbol substituteGenerics(
            Map<GenericSymbol, Type> genericSubstitutions,
            Scope substitutionScope) {
        FunctionSymbol thisWithGenericsSubstituted =
                new FunctionSymbol(name, tree, scopeRepo, rootModuleID);
        if (!(getType() instanceof GenericSymbol)) {
            thisWithGenericsSubstituted.setType(getType());
        }
        thisWithGenericsSubstituted.setEnclosingScope(substitutionScope);
        for (Symbol nestedSyms : getSymbols()) {
            try {
                thisWithGenericsSubstituted.define(
                        nestedSyms.substituteGenerics(genericSubstitutions,
                                thisWithGenericsSubstituted));
            }
            catch (DuplicateSymbolException dse) {
                //shouldn't happen.
            }
        }
        return thisWithGenericsSubstituted;
    }

}
