package resolvelite.semantics;

public class Symbol {
    public static enum Type {tINVALID, tVOID, tINT, tFLOAT}

    protected final String name;      // All symbols at least have a name
    protected Type type;
    protected Scope scope;      // All symbols know what scope contains them.

    public Symbol(String name) { this.name = name; }
    public Symbol(String name, Type type) { this(name); this.type = type; }
    public String getName() { return name; }

    public String toString() {
        if ( type!=Type.tINVALID ) return '<'+getName()+":"+type+'>';
        return getName();
    }
}
