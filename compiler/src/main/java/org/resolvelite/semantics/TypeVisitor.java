package org.resolvelite.semantics;

public abstract class TypeVisitor {

    public void beginMTType(MTType t) {}

    public void beginMTInvalid(MTInvalid t) {}

    public void beginMTAbstract(MTAbstract<?> t) {}

    //public void beginMTBigUnion(MTBigUnion t) {}

    public void beginMTCartesian(MTCartesian t) {}

    public void beginMTFunction(MTFunction t) {}

    public void beginMTFunctionApplication(MTFunctionApplication t) {}

    //public void beginMTIntersect(MTIntersect t) {}

    public void beginMTPowersetApplication(MTPowersetApplication t) {}

    public void beginMTProper(MTProper t) {}

    //public void beginMTSetRestriction(MTSetRestriction t) {}

    public void beginMTUnion(MTUnion t) {}

    public void beginMTNamed(MTNamed t) {}

    public void beginChildren(MTType t) {}

    public void endChildren(MTType t) {}

    public void endMTType(MTType t) {}

    public void endMTAbstract(MTAbstract<?> t) {}

    //public void endMTBigUnion(MTBigUnion t) {}

    public void endMTCartesian(MTCartesian t) {}

    public void endMTFunction(MTFunction t) {}

    public void endMTFunctionApplication(MTFunctionApplication t) {}

    //public void endMTIntersect(MTIntersect t) {}

    public void endMTPowersetApplication(MTPowersetApplication t) {}

    public void endMTProper(MTProper t) {}

    //public void endMTSetRestriction(MTSetRestriction t) {}

    public void endMTUnion(MTUnion t) {}

    public void endMTNamed(MTNamed t) {}

    public void endMTInvalid(MTInvalid t) {}
}