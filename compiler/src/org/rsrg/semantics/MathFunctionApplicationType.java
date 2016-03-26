package org.rsrg.semantics;

import edu.clemson.resolve.misc.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MathFunctionApplicationType extends MathType {

    private final List<MathType> arguments = new ArrayList<>();
    private final MathFunctionType function;
    private final String name;

    public MathFunctionApplicationType(@NotNull DumbTypeGraph g,
                                       @NotNull MathFunctionType f,
                                       @NotNull String name,
                                       @NotNull MathType ... arguments) {
        this(g, f, name, Arrays.asList(arguments));
    }

    public MathFunctionApplicationType(@NotNull DumbTypeGraph g,
                                       @NotNull MathFunctionType f,
                                       @NotNull String name,
                                       @NotNull List<MathType> arguments) {
        //our range type is the result of an application, so we implicitly exhaust one layer from it...
        super(g, f.getResultType());
        this.function = f;
        this.name = name;
        this.arguments.addAll(arguments);
        this.typeRefDepth = f.getResultType().typeRefDepth - 1;
    }

    public String getName() {
        return name;
    }

    public List<MathType> getComponentTypes() {
        List<MathType> result = new ArrayList<>();
        result.addAll(arguments);
        result.add(function);
        return result;
    }

    @Override public MathType withVariablesSubstituted(
            Map<MathType, MathType> substitutions) {
        MathFunctionType newNameType =
                (MathFunctionType)function
                        .withVariablesSubstituted(substitutions);
        List<MathType> newArgs = new ArrayList<>();
        for (MathType t : arguments) {
            newArgs.add(t.withVariablesSubstituted(substitutions));
        }
        return new MathFunctionApplicationType(g, newNameType, name, newArgs);
    }

    @Override public String toString() {
        return name + "(" + Utils.join(arguments, ", ") + ")";
    }

}
