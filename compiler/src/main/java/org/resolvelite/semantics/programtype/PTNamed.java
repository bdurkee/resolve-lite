package org.resolvelite.semantics.programtype;

import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.typereasoning.TypeGraph;

/**
 * Named types for which we know initialization and exemplar info. Note then
 * that this does not include generics.
 */
public abstract class PTNamed extends PTType {

    private final String name;
    protected final ResolveParser.EnsuresClauseContext initEnsures;

    /**
     * Which module does this {@code PTType}s reference appear in?
     */
    private final String enclosingModuleID;

    public PTNamed(TypeGraph g, String name,
            ResolveParser.EnsuresClauseContext initEnsures,
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

    public ResolveParser.EnsuresClauseContext getInitializationEnsures() {
        return initEnsures;
    }

    @Override public String toString() {
        return name;
    }

}
