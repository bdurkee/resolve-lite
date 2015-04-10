package org.resolvelite.semantics;

import org.resolvelite.typereasoning.TypeGraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class MTFunctionApplication extends MTType {

    private final MTFunction function;
    private List<MTType> arguments;
    private String name;

    private List<MTType> components;

    public MTFunctionApplication(TypeGraph g, MTFunction f, String name,
                                 MTType... arguments) {
        super(g);

        this.function = f;
        this.name = name;
        this.arguments = new ArrayList<>();
        for (int i = 0; i < arguments.length; ++i) {
            this.arguments.add(arguments[i]);
        }

        setUpComponents();
    }

    public MTFunctionApplication(TypeGraph g, MTFunction f, String name,
                                 List<MTType> arguments) {
        super(g);

        this.function = f;
        this.name = name;
        this.arguments = new ArrayList<MTType>();
        this.arguments.addAll(arguments);

        setUpComponents();
    }

    private void setUpComponents() {
        List<MTType> result = new ArrayList<MTType>();

        result.add(function);
        result.addAll(arguments);

        components = Collections.unmodifiableList(result);
    }

    @Override
    public boolean isKnownToContainOnlyMTypes() {
        //Note that, effectively, we represent an instance of the range of our
        //function.  Thus, we're known to contain only MTypes if the function's
        //range's members are known only to contain MTypes.

        return function.getRange().membersKnownToContainOnlyMTypes();
    }

    @Override
    public boolean membersKnownToContainOnlyMTypes() {
        boolean result = true;
        Iterator<MTType> arguments = arguments.iterator();
        while (arguments.hasNext()) {
            result &= arguments.next().isKnownToContainOnlyMathTypes();
        }
        return result
                && myFunction
                .applicationResultsKnownToContainOnlyRestrictions();
    }
}
