package org.resolvelite.semantics.programtype;

import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.semantics.MTType;
import org.resolvelite.typereasoning.TypeGraph;

/**
 * Named types for which we know initialization and exemplar info. Note then
 * that this does not include generics.
 */
public abstract class PTNamed extends PTType {

    private final String name;
    protected final PExp initRequires, initEnsures, finalRequires,
            finalEnsures;

    public PTNamed(TypeGraph g, String name, PExp initRequires,
            PExp initEnsures, PExp finalRequires, PExp finalEnsures) {
        super(g);
        this.name = name;
        this.initRequires = initRequires;
        this.initEnsures = initEnsures;
        this.finalRequires = finalRequires;
        this.finalEnsures = finalEnsures;
    }

    public String getName() {
        return name;
    }

    public abstract String getExemplarName();

    public PExp getInitializationRequires() {
        return initRequires;
    }

    public PExp getInitializationEnsures() {
        return initEnsures;
    }

    public PExp getFinalizationRequires() {
        return finalRequires;
    }

    public PExp getFinalizationEnsures() {
        return finalEnsures;
    }

}
