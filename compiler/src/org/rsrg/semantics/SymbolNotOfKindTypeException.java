package org.rsrg.semantics;

import edu.clemson.resolve.compiler.ErrorKind;

@SuppressWarnings("serial")
public class SymbolNotOfKindTypeException extends SymbolTableException {

    public SymbolNotOfKindTypeException() {
        super(ErrorKind.INVALID_MATH_TYPE);
    }
}
