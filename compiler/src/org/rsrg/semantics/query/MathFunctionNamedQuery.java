package org.rsrg.semantics.query;

import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.*;
import org.rsrg.semantics.MathSymbolTable.FacilityStrategy;
import org.rsrg.semantics.MathSymbolTable.ImportStrategy;
import org.rsrg.semantics.symbol.MathSymbol;
import org.rsrg.semantics.symbol.Symbol;

import java.util.ArrayList;
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
            throws NoSuchModuleException, UnexpectedSymbolException {
        List<Symbol> intermediateList;
        try {
            intermediateList = nameQuery.searchFromContext(source, repo);
        }
        catch (DuplicateSymbolException dse) {
            //Shouldn't be possible
            throw new RuntimeException(dse);
        }
        List<MathSymbol> resultingList = new ArrayList<>();
        for (Symbol sym : intermediateList) {
            resultingList.add(sym.toMathSymbol());
        }
        return resultingList;
    }
}