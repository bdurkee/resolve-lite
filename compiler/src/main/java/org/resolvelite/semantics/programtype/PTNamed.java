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
    protected final ResolveParser.RequiresClauseContext initRequires;
    protected final ResolveParser.EnsuresClauseContext initEnsures;
    protected final ResolveParser.RequiresClauseContext finalRequires;
    protected final ResolveParser.EnsuresClauseContext finalEnsures;
    /**
     * Which module does this {@code PTType}s reference appear in?
     */
    private final String enclosingModuleID;

    public PTNamed(TypeGraph g, String name,
            ResolveParser.RequiresClauseContext initRequires,
            ResolveParser.EnsuresClauseContext initEnsures,
            ResolveParser.RequiresClauseContext finalRequires,
            ResolveParser.EnsuresClauseContext finalEnsures,
            String enclosingModuleID) {
        super(g);
        this.name = name;
        this.initRequires = initRequires;
        this.initEnsures = initEnsures;
        this.finalRequires = finalRequires;
        this.finalEnsures = finalEnsures;
        this.enclosingModuleID = enclosingModuleID;
    }

    public String getEnclosingModuleID() {
        return enclosingModuleID;
    }

    public String getName() {
        return name;
    }

    public abstract String getExemplarName();

    public ResolveParser.RequiresClauseContext getInitializationRequires() {
        return initRequires;
    }

    public ResolveParser.EnsuresClauseContext getInitializationEnsures() {
        return initEnsures;
    }

    public ResolveParser.RequiresClauseContext getFinalizationRequires() {
        return finalRequires;
    }

    public ResolveParser.EnsuresClauseContext getFinalizationEnsures() {
        return finalEnsures;
    }

    @Override public String toString() {
        return name;
    }

}
