package org.rsrg.semantics;

import edu.clemson.resolve.compiler.ErrorKind;
import org.rsrg.semantics.symbol.Symbol;

@SuppressWarnings("serial")
public class DuplicateSymbolException extends SymbolTableException {

    private Symbol existingSymbol;

    public DuplicateSymbolException(Symbol existingSym) {
        super(ErrorKind.DUP_SYMBOL);
        this.existingSymbol = existingSym;
    }

    public DuplicateSymbolException() {
        this(null);
    }

    public Symbol getExistingSymbol() {
        return existingSymbol;
    }

}
