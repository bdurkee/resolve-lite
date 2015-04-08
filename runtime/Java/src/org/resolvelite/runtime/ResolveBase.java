package org.resolvelite.runtime;

public class ResolveBase {
    // ((Test.Baz)((((Stack)S).rep.Contents))).rep.Fiz;

    public static void swap(RType r1, RType r2) {
        Object tmp = r1.getRep();
        r1.setRep(r2.getRep());
        r2.setRep(tmp);
    }

    public static void assign(RType r1, RType r2) {
        r1.setRep(r2.getRep());
    }
}