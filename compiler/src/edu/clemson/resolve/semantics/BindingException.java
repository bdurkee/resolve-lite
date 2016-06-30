package edu.clemson.resolve.semantics;

public class BindingException extends Exception {

    private static final long serialVersionUID = 1L;

    public final MathClssftn found, expected;

    public BindingException(MathClssftn found, MathClssftn expected) {
        this.found = found;
        this.expected = expected;
    }
}
