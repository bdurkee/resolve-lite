package edu.clemson.resolve.semantics;

import edu.clemson.resolve.compiler.ErrorKind;

@SuppressWarnings("serial")
public class NoSuchSymbolException extends SymbolTableException {

    public NoSuchSymbolException() {
        super(ErrorKind.NO_SUCH_SYMBOL);
    }

}