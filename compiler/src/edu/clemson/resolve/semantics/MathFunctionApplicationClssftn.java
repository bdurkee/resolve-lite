package edu.clemson.resolve.semantics;

import edu.clemson.resolve.misc.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MathFunctionApplicationClssftn extends MathClssftn {

    private final List<MathClssftn> arguments = new ArrayList<>();
    private final MathFunctionClssftn function;
    private final String name;

    public MathFunctionApplicationClssftn(@NotNull DumbMathClssftnHandler g,
                                          @NotNull MathFunctionClssftn f,
                                          @NotNull String name,
                                          @NotNull MathClssftn... arguments) {
        this(g, f, name, Arrays.asList(arguments));
    }

    public MathFunctionApplicationClssftn(@NotNull DumbMathClssftnHandler g,
                                          @NotNull MathFunctionClssftn f,
                                          @NotNull String name,
                                          @NotNull List<MathClssftn> arguments) {
        //our range type is the result of an application, so we implicitly exhaust one layer from it...
        super(g, f.getRangeClssftn());
        this.function = f;
        this.name = name;
        this.arguments.addAll(arguments);
        this.typeRefDepth = f.getRangeClssftn().typeRefDepth - 1;
    }

    public String getName() {
        return name;
    }

    public List<MathClssftn> getComponentTypes() {
        List<MathClssftn> result = new ArrayList<>();
        result.addAll(arguments);
        result.add(function);
        return result;
    }

    @NotNull
    public MathFunctionClssftn getFunction() {
        return function;
    }

    @Override
    public MathClssftn withVariablesSubstituted(Map<String, MathClssftn> substitutions) {
        MathFunctionClssftn newNameType =
                (MathFunctionClssftn) function.withVariablesSubstituted(substitutions);
        List<MathClssftn> newArgs = new ArrayList<>();
        for (MathClssftn t : arguments) {
            newArgs.add(t.withVariablesSubstituted(substitutions));
        }
        return new MathFunctionApplicationClssftn(g, newNameType, name, newArgs);
    }

    @Override
    public String toString() {
        return name + "(" + Utils.join(arguments, ", ") + ")";
    }

}
