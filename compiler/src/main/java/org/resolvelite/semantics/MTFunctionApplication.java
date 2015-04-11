package org.resolvelite.semantics;

import org.resolvelite.typereasoning.TypeGraph;

import java.util.*;

public class MTFunctionApplication extends MTType {

    private final MTFunction function;
    protected final List<MTType> arguments = new ArrayList<>();
    private String name;

    private List<MTType> components = new ArrayList<>();

    public MTFunctionApplication(TypeGraph g, MTFunction f, String name,
            MTType... arguments) {
        super(g);

        this.function = f;
        this.name = name;
        this.arguments.addAll(Arrays.asList(arguments));
        this.components.add(function);
        this.components.addAll(this.arguments);
    }

    public MTFunctionApplication(TypeGraph g, MTFunction f, String name,
            List<MTType> arguments) {
        super(g);

        this.function = f;
        this.name = name;
        this.arguments.addAll(arguments);
        this.components.add(function);
        this.components.addAll(arguments);
    }

    @Override public List<MTType> getComponentTypes() {
        return components;
    }

    @Override public boolean isKnownToContainOnlyMathTypes() {
        //Note that, effectively, we represent an instance of the range of our
        //function.  Thus, we're known to contain only MTypes if the function's
        //range's members are known only to contain MTypes.
        return function.getRange().membersKnownToContainOnlyMathTypes();
    }

    @Override public boolean membersKnownToContainOnlyMathTypes() {
        boolean result = true;
        for (MTType argument : this.arguments) {
            result &= argument.isKnownToContainOnlyMathTypes();
        }
        return result
                && function.applicationResultsKnownToContainOnlyRestrictions();
    }
}
