package org.rsrg.semantics.query;

import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.DuplicateSymbolException;
import org.rsrg.semantics.MathSymbolTable;
import org.rsrg.semantics.NoSuchModuleException;
import org.rsrg.semantics.Scope;
import org.rsrg.semantics.MathSymbolTable.FacilityStrategy;
import org.rsrg.semantics.MathSymbolTable.ImportStrategy;
import org.rsrg.semantics.symbol.MathSymbol;
import org.rsrg.semantics.symbol.Symbol;

import java.util.List;
import java.util.stream.Collectors;

import static org.rsrg.semantics.MathSymbolTable.FacilityStrategy.FACILITY_IGNORE;
import static org.rsrg.semantics.MathSymbolTable.ImportStrategy.IMPORT_RECURSIVE;

public class MathFunctionNamedQuery
        implements
            MultimatchSymbolQuery<MathSymbol> {

   @NotNull private final SymbolQuery<Symbol> nameQuery;

    public MathFunctionNamedQuery(@Nullable Token qualifier,
                                  @NotNull Token name) {
        this.nameQuery =
                new NameQuery(qualifier, name.getText(),
                        IMPORT_RECURSIVE, FACILITY_IGNORE, false);
    }

    @Override public List<MathSymbol> searchFromContext(@NotNull Scope source,
                                                        @NotNull MathSymbolTable repo)
            throws NoSuchModuleException {
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