package org.resolvelite.semantics.query;

import org.resolvelite.semantics.DuplicateSymbolException;
import org.resolvelite.semantics.Scope;
import org.resolvelite.semantics.SymbolTable;
import org.resolvelite.semantics.SymbolTable.FacilityStrategy;
import org.resolvelite.semantics.SymbolTable.ImportStrategy;
import org.antlr.v4.runtime.Token;
import org.resolvelite.semantics.symbol.MathSymbol;
import org.resolvelite.semantics.symbol.Symbol;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MathFunctionNamedQuery
        implements
            MultimatchSymbolQuery<MathSymbol> {

    private final SymbolQuery<Symbol> nameQuery;

    public MathFunctionNamedQuery(Token qualifier, Token name) {
        this.nameQuery =
                new NameQuery(qualifier, name.getText(),
                        ImportStrategy.IMPORT_RECURSIVE,
                        FacilityStrategy.FACILITY_IGNORE, false);
    }

    @Override public List<MathSymbol> searchFromContext(Scope source,
                                                   SymbolTable repo) {
        List<Symbol> intermediateList;
        try {
            intermediateList = nameQuery.searchFromContext(source, repo);
        }
        catch (DuplicateSymbolException dse) {
            //Shouldn't be possible
            throw new RuntimeException(dse);
        }
        return intermediateList.stream()
                .map(Symbol::toMathSymbol)
                .collect(Collectors.toList());
    }
}