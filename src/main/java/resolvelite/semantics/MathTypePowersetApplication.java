package resolvelite.semantics;

import resolvelite.typereasoning.TypeGraph;

public class MathTypePowersetApplication extends MathTypeFuncApplication {

    public MathTypePowersetApplication(TypeGraph g, MathType argument) {
        super(g, g.POWERSET, "Powerset", argument);
    }

    @Override
    public boolean isKnownToContainOnlyThingsThatAreTypes() {
        //powerset is by definition a container of containers
        return true;
    }

    @Override
    public boolean membersKnownToContainOnlyThingsThatAreTypes() {
        //I'm the container of all sub-containers of my argument.  My members
        //are containers of members from the original argument.
        return getArguments().get(0).isKnownToContainOnlyThingsThatAreTypes();
    }
}
