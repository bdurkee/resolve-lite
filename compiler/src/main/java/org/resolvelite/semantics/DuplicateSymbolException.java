package org.resolvelite.semantics;

import org.resolvelite.semantics.symbol.Symbol;

@SuppressWarnings("serial")
public class DuplicateSymbolException extends SymbolTableException {

    private final Symbol existingSymbol;

    public DuplicateSymbolException() {
        super();
        this.existingSymbol = null;
    }

    public DuplicateSymbolException(String s) {
        super(s);
        this.existingSymbol = null;
    }

    public DuplicateSymbolException(Symbol existing) {
        super();
        this.existingSymbol = existing;
    }

    public DuplicateSymbolException(Symbol existing, String msg) {
        super(msg);
        this.existingSymbol = existing;
    }

    public Symbol getExistingEntry() {
        return existingSymbol;
    }
}
