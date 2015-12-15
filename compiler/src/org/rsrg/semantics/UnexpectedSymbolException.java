package org.rsrg.semantics;

import org.jetbrains.annotations.NotNull;

public class UnexpectedSymbolException extends RuntimeException {

    @NotNull private final String actualSymbolDescription;

    public UnexpectedSymbolException(@NotNull String actualSymbolDescription) {
        super();
        this.actualSymbolDescription = actualSymbolDescription;
    }

    @NotNull public String getActualSymbolDescription() {
        return actualSymbolDescription;
    }
}
