package org.rsrg.semantics;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface FunctionApplicationFactory {

    public MathType buildFunctionApplication(@NotNull DumbTypeGraph g,
                                             @NotNull MathFunctionType f,
                                             @NotNull String calledAsName,
                                             @NotNull List<MathType> arguments);
}