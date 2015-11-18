package org.rsrg.semantics;

import org.antlr.v4.runtime.Token;

public class NoSuchModuleException extends RuntimeException {

    public final Token requestedModule;

    public NoSuchModuleException(Token requestedModule) {
        this.requestedModule = requestedModule;
    }
}
