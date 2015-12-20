package org.rsrg.semantics;

public abstract class TypeVisitor {

    public void beginMTType(MTType t) {}

    public void beginMTInvalid(MTInvalid t) {}

    public void beginMTCartesian(MTCartesian t) {}

    public void beginMTFunction(MTFunction t) {}

    public void beginMTFunctionApplication(MTFunctionApplication t) {}

    public void beginMTPowersetApplication(MTPowersetApplication t) {}

    public void beginMTProper(MTProper t) {}

    public void beginMTNamed(MTNamed t) {}

    public void beginChildren(MTType t) {}

    public void endChildren(MTType t) {}

    public void endMTType(MTType t) {}

    public void endMTCartesian(MTCartesian t) {}

    public void endMTFunction(MTFunction t) {}

    public void endMTFunctionApplication(MTFunctionApplication t) {}

    public void endMTPowersetApplication(MTPowersetApplication t) {}

    public void endMTProper(MTProper t) {}

    public void endMTNamed(MTNamed t) {}

    public void endMTInvalid(MTInvalid t) {}
}
