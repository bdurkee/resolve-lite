package edu.clemson.resolve.semantics;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface FunctionApplicationFactory {

    public MathClssftn buildFunctionApplication(@NotNull DumbMathClssftnHandler g,
                                                @NotNull MathFunctionClssftn f,
                                                @NotNull String calledAsName,
                                                @NotNull List<MathClssftn> arguments);
}