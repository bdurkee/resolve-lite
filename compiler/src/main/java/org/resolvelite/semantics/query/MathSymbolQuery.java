package org.resolvelite.semantics.query;

import org.antlr.v4.runtime.Token;
import org.resolvelite.semantics.PossiblyQualifiedPath;
import org.resolvelite.semantics.SymbolTable;
import org.resolvelite.semantics.SymbolTable.ImportStrategy;
import org.resolvelite.semantics.SymbolTable.FacilityStrategy;
import org.resolvelite.semantics.searchers.NameSearcher;
import org.resolvelite.semantics.symbol.MathSymbol;
import org.resolvelite.semantics.symbol.Symbol;

public class MathSymbolQuery
        extends
        ResultProcessingQuery<SymbolTableEntry, MathSymbolEntry> {

    public MathSymbolQuery(Token qualifier, Token name) {
        this(qualifier, name.getText(), name);
    }

    public MathSymbolQuery(Token qualifier, String name, Token l) {
        super(new BaseSymbolQuery<SymbolTableEntry>(new PossiblyQualifiedPath(
                qualifier, ImportStrategy.IMPORT_NAMED,
                FacilityStrategy.FACILITY_IGNORE, true), new NameSearcher(name,
                true)), new MapToMathSymbol(l));
    }

    private static class MapToMathSymbol
            implements
            Mapping<Symbol, MathSymbol> {

        private final Location myNameLocation;

        public MapToMathSymbol(Location l) {
            myNameLocation = l;
        }

        @Override
        public MathSymbolEntry map(SymbolTableEntry input) {
            return input.toMathSymbolEntry(myNameLocation);
        }
    }
}