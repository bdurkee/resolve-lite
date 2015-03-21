package resolvelite.semantics;

import org.antlr.v4.runtime.misc.NotNull;
import resolvelite.typereasoning.TypeGraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MathTypeFuncApplication extends MathType {

    private final String name;
    private final MathTypeFunc func;
    private final List<MathType> arguments = new ArrayList<>();

    public MathTypeFuncApplication(TypeGraph g, MathTypeFunc f, String name,
                                   List<MathType> arguments) {
        super(g);
        this.name = name;
        this.func = f;
        this.arguments.addAll(arguments);
    }

    public MathTypeFuncApplication(TypeGraph g, MathTypeFunc f, String name,
                                   MathType... arguments) {
        this(g, f, name, Arrays.asList(arguments));
    }

    public List<MathType> getArguments() {
        return arguments;
    }

    @Override
    public boolean isKnownToContainOnlyThingsThatAreTypes() {
        //Note that, effectively, we represent an instance of the range of our
        //function.  Thus, we're known to contain only MTypes if the function's
        //range's members are known only to contain MTypes.
        return func.getRange().membersKnownToContainOnlyThingsThatAreTypes();
    }

    public interface FunctionApplicationFactory {

        public MathType buildFunctionApplication(@NotNull TypeGraph g,
                @NotNull MathTypeFunc f, @NotNull String refName,
                List<MathType> args);
    }
}
