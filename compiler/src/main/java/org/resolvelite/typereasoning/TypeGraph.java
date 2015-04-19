package org.resolvelite.typereasoning;

import org.resolvelite.semantics.MTProper;

public class TypeGraph {

    public final MTProper CLS = new MTProper(this, null, true, "MType");
    public final MTProper SSET = new MTProper(this, CLS, true, "SSet");
    public final MTProper BOOLEAN = new MTProper(this, SSET, false, "B");
    public final MTProper VOID = new MTProper(this, SSET, false, "Void");
    public final MTProper ENTITY = new MTProper(this, "Entity");

}
