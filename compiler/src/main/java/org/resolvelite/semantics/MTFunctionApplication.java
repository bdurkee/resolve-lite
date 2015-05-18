package org.resolvelite.semantics;

import org.resolvelite.misc.Utils;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.*;

public class MTFunctionApplication extends MTAbstract<MTFunctionApplication> {

    private static final int BASE_HASH = "MTFunctionApplication".hashCode();

    private final MTFunction function;
    protected final List<MTType> arguments;
    private String name;

    private List<MTType> components;

    public MTFunctionApplication(TypeGraph g, MTFunction f, String name,
            MTType... arguments) {
        super(g);
        this.function = f;
        this.name = name;
        this.arguments = Arrays.asList(arguments);
        setUpComponents();
    }

    public MTFunctionApplication(TypeGraph g, MTFunction f, String name,
            List<MTType> arguments) {
        super(g);
        this.function = f;
        this.name = name;
        this.arguments = new ArrayList<MTType>(arguments);
        setUpComponents();
    }

    public MTFunctionApplication(TypeGraph g, MTFunction f,
            List<MTType> arguments) {
        super(g);

        this.function = f;
        this.arguments = new LinkedList<MTType>(arguments);
        this.name = "\\lambda";

        setUpComponents();
    }

    private void setUpComponents() {
        List<MTType> result = new ArrayList<>();
        result.add(this.function);
        result.addAll(this.arguments);
        this.components = result;
    }

    @Override public boolean isKnownToContainOnlyMTypes() {
        //Note that, effectively, we represent an instance of the range of our
        //function.  Thus, we're known to contain only MTypes if the function's
        //range's members are known only to contain MTypes.

        return function.getRange().membersKnownToContainOnlyMTypes();
    }

    public String getName() {
        return name;
    }

    @Override public List<MTType> getComponentTypes() {
        return components;
    }

    public MTFunction getFunction() {
        return function;
    }

    public List<MTType> getArguments() {
        return Collections.unmodifiableList(arguments);
    }

    @Override public MTType withComponentReplaced(int index, MTType newType) {
        MTFunction newFunction = function;
        List<MTType> newArguments = arguments;

        if ( index == 0 ) {
            newFunction = (MTFunction) newType;
        }
        else {
            newArguments = new LinkedList<MTType>(newArguments);
            newArguments.set(index - 1, newType);
        }

        return new MTFunctionApplication(getTypeGraph(), newFunction, name,
                newArguments);
    }

    @Override public String toString() {
        StringBuffer sb = new StringBuffer();
        if ( arguments.size() == 2 ) {
            sb.append(arguments.get(0).toString());
            sb.append(" ");
            sb.append(name);
            sb.append(" ");
            sb.append(arguments.get(1).toString());
        }
        else if ( arguments.size() == 1 && name.contains("_") ) {
            // ^^^ super hacky way to detect outfix
            sb.append(name.replace("_", arguments.get(0).toString()));
        }
        else {
            sb.append(name);
            sb.append("(").append(Utils.join(arguments, ", ")).append(")");
        }
        return sb.toString();
    }

    @Override public void acceptOpen(TypeVisitor v) {
        v.beginMTType(this);
        v.beginMTAbstract(this);
        v.beginMTFunctionApplication(this);
    }

    @Override public void accept(TypeVisitor v) {
        acceptOpen(v);
        v.beginChildren(this);
        function.accept(v);

        for (MTType arg : arguments) {
            arg.accept(v);
        }

        v.endChildren(this);
        acceptClose(v);
    }

    @Override public void acceptClose(TypeVisitor v) {
        v.endMTFunctionApplication(this);
        v.endMTAbstract(this);
        v.endMTType(this);
    }

    @Override public int getHashCode() {
        int result = BASE_HASH + function.getHashCode() + name.hashCode();
        for (MTType t : arguments) {
            result *= 73;
            result += t.getHashCode();
        }
        return result;
    }

}
