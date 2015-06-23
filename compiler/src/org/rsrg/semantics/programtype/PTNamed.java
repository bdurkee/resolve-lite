package org.rsrg.semantics.programtype;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.typereasoning.TypeGraph;

/**
 * Named types for which we know initialization and exemplar info. Note then
 * that this does not include generics.
 */
public abstract class PTNamed extends PTType {

    private final String name;
    protected final PExp initEnsures;

    /**
     * Which module does this {@code PTType}s reference appear in?
     */
    private final String enclosingModuleID;

    public PTNamed(TypeGraph g, String name, PExp initEnsures,
            String enclosingModuleID) {
        super(g);
        this.name = name;
        this.initEnsures = initEnsures;
        this.enclosingModuleID = enclosingModuleID;
    }

    public String getEnclosingModuleID() {
        return enclosingModuleID;
    }

    public String getName() {
        return name;
    }

    public abstract String getExemplarName();

    public PExp getInitializationEnsures() {
        return initEnsures;
    }

    @Override public String toString() {
        return name;
    }

}
