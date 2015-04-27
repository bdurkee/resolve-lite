package org.resolvelite.semantics;

import org.resolvelite.compiler.ErrorKind;

@SuppressWarnings("serial")
public class NoSuchSymbolException extends SymbolTableException {

    public NoSuchSymbolException() {
        super(ErrorKind.NO_SUCH_SYMBOL);
    }

}