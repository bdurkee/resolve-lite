package org.resolvelite.semantics;

public class UnexpectedSymbolException extends RuntimeException {

    public UnexpectedSymbolException() {
        super();
    }

    public UnexpectedSymbolException(String msg) {
        super(msg);
    }

    public UnexpectedSymbolException(Exception causedBy) {
        super(causedBy);
    }
}
