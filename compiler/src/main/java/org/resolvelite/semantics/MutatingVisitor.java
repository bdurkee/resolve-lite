package org.resolvelite.semantics;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class MutatingVisitor extends BoundVariableVisitor {

    private MTType myRoot;
    protected MTType myFinalExpression;

    public MTType getFinalExpression() {
        return myFinalExpression;
    }

    @Override public final void beginMTType(MTType t) {
        if ( myRoot == null ) {
            myRoot = t;
            myFinalExpression = myRoot;
        }
        mutateBeginMTType(t);
    }

    public void mutateBeginMTType(MTType t) {}

    public void mutateEndMTType(MTType t) {}

    @Override public final void endChildren(MTType t) {
        mutateEndChildren(t);
    }

    public void mutateEndChildren(MTType t) {}

    @Override public final void endMTType(MTType t) {
        mutateEndMTType(t);
    }
}
