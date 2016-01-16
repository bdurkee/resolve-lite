package org.rsrg.semantics;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface FunctionApplicationFactory {

    public MTType buildFunctionApplication(@NotNull TypeGraph g,
                                           @NotNull MTFunction f,
                                           @NotNull String calledAsName,
                                           @NotNull List<MTType> arguments);
}