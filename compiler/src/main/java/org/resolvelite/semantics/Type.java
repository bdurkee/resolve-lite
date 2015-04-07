package org.resolvelite.semantics;

public interface Type {
    public String getName();

    public String getRootModuleID(); //Todo: not sure if this is the best.
    //Might just get rid of it, though it does make translation of
    //member exps easier..
}
