package edu.clemson.resolve.semantics;

import edu.clemson.resolve.misc.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MathFunctionApplicationClassification extends MathClassification {

    private final List<MathClassification> arguments = new ArrayList<>();
    private final MathFunctionClassification function;
    private final String name;

    public MathFunctionApplicationClassification(@NotNull DumbTypeGraph g,
                                                 @NotNull MathFunctionClassification f,
                                                 @NotNull String name,
                                                 @NotNull MathClassification... arguments) {
        this(g, f, name, Arrays.asList(arguments));
    }

    public MathFunctionApplicationClassification(@NotNull DumbTypeGraph g,
                                                 @NotNull MathFunctionClassification f,
                                                 @NotNull String name,
                                                 @NotNull List<MathClassification> arguments) {
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

    public List<MathClassification> getComponentTypes() {
        List<MathClassification> result = new ArrayList<>();
        result.addAll(arguments);
        result.add(function);
        return result;
    }

    @Override
    public MathClassification withVariablesSubstituted(
            Map<String, MathClassification> substitutions) {
        MathFunctionClassification newNameType =
                (MathFunctionClassification) function
                        .withVariablesSubstituted(substitutions);
        List<MathClassification> newArgs = new ArrayList<>();
        for ( MathClassification t : arguments ) {
            newArgs.add(t.withVariablesSubstituted(substitutions));
        }
        return new MathFunctionApplicationClassification(g, newNameType, name, newArgs);
    }

    @Override
    public String toString() {
        return name + "(" + Utils.join(arguments, ", ") + ")";
    }

}
