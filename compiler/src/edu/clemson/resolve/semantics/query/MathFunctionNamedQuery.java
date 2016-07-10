package edu.clemson.resolve.semantics.query;

import edu.clemson.resolve.semantics.*;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import edu.clemson.resolve.semantics.symbol.MathClssftnWrappingSymbol;
import edu.clemson.resolve.semantics.symbol.Symbol;

import java.util.ArrayList;
import java.util.List;

import static edu.clemson.resolve.semantics.MathSymbolTable.FacilityStrategy.FACILITY_IGNORE;
import static edu.clemson.resolve.semantics.MathSymbolTable.ImportStrategy.IMPORT_RECURSIVE;

public class MathFunctionNamedQuery implements MultimatchSymbolQuery<MathClssftnWrappingSymbol> {

    @NotNull
    private final SymbolQuery<Symbol> nameQuery;

    public MathFunctionNamedQuery(@Nullable Token qualifier, @NotNull Token name) {
        this.nameQuery = new NameQuery(qualifier, name.getText(), IMPORT_RECURSIVE, FACILITY_IGNORE, false);
    }

    @Override
    public List<MathClssftnWrappingSymbol> searchFromContext(@NotNull Scope source,
                                                             @NotNull MathSymbolTable repo)
            throws NoSuchModuleException, UnexpectedSymbolException {
        List<Symbol> intermediateList;
        try {
            intermediateList = nameQuery.searchFromContext(source, repo);
        } catch (DuplicateSymbolException dse) {
            //Shouldn't be possible
            throw new RuntimeException(dse);
        }
        List<MathClssftnWrappingSymbol> resultingList = new ArrayList<>();
        for (Symbol sym : intermediateList) {
            resultingList.add(sym.toMathSymbol());
        }
        return resultingList;
    }
}