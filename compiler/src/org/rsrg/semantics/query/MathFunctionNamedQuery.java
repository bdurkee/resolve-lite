package org.rsrg.semantics.query;

import org.antlr.v4.runtime.Token;
import org.rsrg.semantics.DuplicateSymbolException;
import org.rsrg.semantics.Scope;
import org.rsrg.semantics.SymbolTable;
import org.rsrg.semantics.SymbolTable.FacilityStrategy;
import org.rsrg.semantics.SymbolTable.ImportStrategy;
import org.rsrg.semantics.symbol.MathSymbol;
import org.rsrg.semantics.symbol.Symbol;

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
        return intermediateList.stream().map(Symbol::toMathSymbol)
                .collect(Collectors.toList());
    }
}