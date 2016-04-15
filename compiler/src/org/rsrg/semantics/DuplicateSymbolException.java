package org.rsrg.semantics;

import edu.clemson.resolve.compiler.ErrorKind;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.symbol.Symbol;

@SuppressWarnings("serial")
public class DuplicateSymbolException extends SymbolTableException {

    private Symbol offendingSymbol;

    public DuplicateSymbolException(@Nullable Symbol offendingSymbol) {
        super(ErrorKind.DUP_SYMBOL);
        this.offendingSymbol = offendingSymbol;
    }

    public DuplicateSymbolException() {
        this(null);
    }

    public Symbol getOffendingSymbol() {
        return offendingSymbol;
    }

}
