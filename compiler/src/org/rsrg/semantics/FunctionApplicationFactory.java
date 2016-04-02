package org.rsrg.semantics;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface FunctionApplicationFactory {

    public MathClassification buildFunctionApplication(@NotNull DumbTypeGraph g,
                                                       @NotNull MathArrowClassification f,
                                                       @NotNull String calledAsName,
                                                       @NotNull List<MathClassification> arguments);
}