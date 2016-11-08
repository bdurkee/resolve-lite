package edu.clemson.resolve.vcgen;

import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;

public final class VC2 {

    /** A human-readable name for the VC; used for debugging purposes. */
    private final int number;
    private final String explanation;
    private final Sequent sequent;

    public VC2(@NotNull Sequent sequent) {
        this.sequent = sequent;
    }

    public int getNumber() {
        return number;
    }

    @NotNull
    public String getExplanation() {
        return explanation;
    }

    @NotNull
    public Token getLocation() {
        return location;
    }
}
