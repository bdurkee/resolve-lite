package org.rsrg.semantics;

public class UnexpectedSymbolException extends RuntimeException {

    private final String actualSymbolDescription;

    public UnexpectedSymbolException(String actualSymbolDescription) {
        super();
        this.actualSymbolDescription = actualSymbolDescription;
    }

    public String getActualSymbolDescription() {
        return actualSymbolDescription;
    }
}
