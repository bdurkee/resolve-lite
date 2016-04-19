package edu.clemson.resolve.semantics;

import edu.clemson.resolve.compiler.ErrorKind;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;

public class NoSuchModuleException extends SymbolTableException {

    @NotNull private final Token requestedModule;

    public NoSuchModuleException(@NotNull ModuleIdentifier requestedModule) {
        this(requestedModule.getNameToken());
    }

    public NoSuchModuleException(@NotNull Token requestedModule) {
        super(ErrorKind.NO_SUCH_MODULE);
        this.requestedModule = requestedModule;
    }

    @NotNull public Token getRequestedModule() {
        return requestedModule;
    }
}
