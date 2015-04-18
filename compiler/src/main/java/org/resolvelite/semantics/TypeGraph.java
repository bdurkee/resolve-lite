package org.resolvelite.semantics;

import org.resolvelite.semantics.symbol.MathSymbol;

public class TypeGraph {

    public final MathSymbol CLS, SSET, B;
    private final PredefinedScope s;
    public TypeGraph(PredefinedScope s) {
        this.s = s;
        CLS = new MathSymbol("Cls", null, true, s.getRootModuleID());
        SSET = new MathSymbol("SSet", CLS, true, s.getRootModuleID());
        B = new MathSymbol("B", SSET, false, s.getRootModuleID());
    }

}
