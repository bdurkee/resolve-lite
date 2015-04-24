package org.resolvelite.typereasoning;

import org.resolvelite.semantics.MTProper;

public class TypeGraph {

    public final MTProper CLS = new MTProper(this, null, true, "Cls");
    public final MTProper SSET = new MTProper(this, CLS, true, "SSet");
    public final MTProper Z = new MTProper(this, SSET, true, "Z");

    public final MTProper BOOLEAN = new MTProper(this, SSET, false, "B");
    public final MTProper VOID = new MTProper(this, SSET, false, "Void");
    public final MTProper ENTITY = new MTProper(this, "Entity");

    public final MTProper MALFORMED = new MTProper(this, "Malformed");

}
