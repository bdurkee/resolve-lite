package org.resolvelite.semantics;

import org.resolvelite.typereasoning.TypeGraph;

public class MTPowersetApplication extends MTFunctionApplication {
    public MTPowersetApplication(TypeGraph g, MTType argument) {
        super(g, g.POWERSET, "Powerset", argument);
    }

    @Override public boolean isKnownToContainOnlyMathTypes() {
        //The powertype is, by definition, a container of containers
        return true;
    }

    @Override public boolean membersKnownToContainOnlyMathTypes() {
        //I'm the container of all sub-containers of my argument.  My members
        //are containers of members from the original argument.
        return arguments.get(0).isKnownToContainOnlyMathTypes();
    }

    @Override public void accept(TypeVisitor v) {
        v.beginMTType(this);
        v.beginMTAbstract(this);
        v.beginMTFunctionApplication(this);
        v.beginMTPowersetApplication(this);

        v.beginChildren(this);

        getFunction().accept(v);

        for (MTType arg : getArguments()) {
            arg.accept(v);
        }

        v.endChildren(this);

        v.endMTPowersetApplication(this);
        v.endMTFunctionApplication(this);
        v.endMTAbstract(this);
        v.endMTType(this);
    }
}
