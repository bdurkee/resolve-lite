package org.rsrg.semantics;

import edu.clemson.resolve.compiler.ErrorKind;
import org.jetbrains.annotations.NotNull;

public class UnexpectedSymbolException extends SymbolTableException {

    @NotNull private final String actualSymbolDescription;

    public UnexpectedSymbolException(@NotNull String actualSymbolDescription) {
        super(ErrorKind.UNEXPECTED_SYMBOL);
        this.actualSymbolDescription = actualSymbolDescription;
    }

    @NotNull public String getActualSymbolDescription() {
        return actualSymbolDescription;
    }
}
