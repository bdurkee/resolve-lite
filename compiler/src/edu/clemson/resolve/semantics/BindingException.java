package edu.clemson.resolve.semantics;

public class BindingException extends Exception {

    private static final long serialVersionUID = 1L;

    public final MathClassification found, expected;

    public BindingException(MathClassification found, MathClassification expected) {
        this.found = found;
        this.expected = expected;
    }
}
