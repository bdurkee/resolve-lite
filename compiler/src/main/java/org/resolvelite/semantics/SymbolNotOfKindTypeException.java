package org.resolvelite.semantics;

import org.resolvelite.compiler.ErrorKind;

@SuppressWarnings("serial")
public class SymbolNotOfKindTypeException extends SymbolTableException {

    public SymbolNotOfKindTypeException() {
        super(ErrorKind.INVALID_MATH_TYPE);
    }
}
