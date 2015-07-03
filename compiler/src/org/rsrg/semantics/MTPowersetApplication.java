package org.rsrg.semantics;

public class MTPowersetApplication extends MTFunctionApplication {

    public MTPowersetApplication(TypeGraph g, MTType argument) {
        super(g, g.POWERSET, "Powerset", argument);
    }

    @Override public boolean isKnownToContainOnlyMTypes() {
        //The powertype is, by definition, a container of containers
        return true;
    }

    @Override public boolean membersKnownToContainOnlyMTypes() {
        //I'm the container of all sub-containers of my argument.  My members
        //are containers of members from the original argument.
        return arguments.get(0).isKnownToContainOnlyMTypes();
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

    @Override public MTType withComponentReplaced(int index, MTType newType) {
        MTType result;

        switch (index) {
        case 0:
            result =
                    new MTFunctionApplication(getTypeGraph(),
                            (MTFunction) newType, getArguments());
            break;
        case 1:
            result = new MTPowersetApplication(getTypeGraph(), newType);
            break;
        default:
            throw new IndexOutOfBoundsException("" + index);
        }

        return result;
    }

    @Override public int getHashCode() {
        return 0;
    }
}
