package resolvelite.typereasoning;

import resolvelite.typeandpopulate.MTProper;
import resolvelite.typeandpopulate.MTType;

public class TypeGraph {

    public final MTType ELEMENT = new MTProper(this, "Element");
    public final MTType ENTITY = new MTProper(this, "Entity");
    public final MTProper CLS = new MTProper(this, null, true, "Cls");

    public final MTProper SET = new MTProper(this, CLS, true, "SSet");
    public final MTProper BOOLEAN = new MTProper(this, CLS, false, "B");
    public final MTProper Z = new MTProper(this, CLS, false, "Z");

}
