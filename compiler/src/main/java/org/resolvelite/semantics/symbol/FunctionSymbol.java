package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.ParserRuleContext;
import org.resolvelite.semantics.DuplicateSymbolException;
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
            Map<GenericSymbol, Type> genericSubstitutions) {
        FunctionSymbol thisWithGenericsSubstituted =
                new FunctionSymbol(name, tree, scopeRepo, rootModuleID);
        thisWithGenericsSubstituted.setEnclosingScope(this.getEnclosingScope());

        if ( this.getType() instanceof GenericSymbol ) { //if the return type itself is generic
            thisWithGenericsSubstituted.setType(genericSubstitutions.get(this
                    .getType()));
        }
        else {
            thisWithGenericsSubstituted.setType(this.getType());
        }

        for (ParameterSymbol p : getSymbolsOfType(ParameterSymbol.class)) {
            try {
                ParameterSymbol newParam =
                        new ParameterSymbol(p.getName(), p.getMode(),
                                this.getEnclosingScope(), rootModuleID);
                if ( p.getType() instanceof GenericSymbol ) {
                    Type g = genericSubstitutions.get(p.getType());
                    if ( g == null ) {
                        throw new RuntimeException(
                                "missing generic instantiation!");
                    }
                    newParam.setType(g);
                }
                else {
                    newParam.setType(p.getType()); //don't touch non generic types.
                }
                thisWithGenericsSubstituted.define(newParam);
            }
            catch (DuplicateSymbolException dse) {
                //shouldn't happen. We've created a fresh scope and we're the
                //only ones that are going to be adding to this particular
                //scoped symbol.
            }
        }
        return thisWithGenericsSubstituted;
    }

}
