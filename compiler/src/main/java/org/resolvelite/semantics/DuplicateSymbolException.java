package org.resolvelite.semantics;

import org.resolvelite.compiler.ErrorKind;
import org.resolvelite.semantics.symbol.Symbol;

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
