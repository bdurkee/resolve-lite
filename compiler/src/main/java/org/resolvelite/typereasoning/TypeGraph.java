package org.resolvelite.typereasoning;

import org.resolvelite.semantics.MTProper;

public class TypeGraph {

    public final MTProper CLS = new MTProper(this, null, true, "MType");

    public final MTProper SET = new MTProper(this, CLS, true, "SSet");
    public final MTProper BOOLEAN = new MTProper(this, CLS, false, "B");
    public final MTProper VOID = new MTProper(this, CLS, false, "Void");

}
