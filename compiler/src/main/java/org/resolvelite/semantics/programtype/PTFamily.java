package org.resolvelite.semantics.programtype;

import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.semantics.MTType;
import org.resolvelite.typereasoning.TypeGraph;

public class PTFamily extends PTType {

    private final MTType model;
    private final String name, exemplarName;

    private final PExp constraint;
    private final PExp initRequires, initEnsures;
    private final PExp finalRequires, finalEnsures;

    public PTFamily(MTType model, String familyName, String exemplarName,
            PExp constraint, PExp initRequires, PExp initEnsures,
            PExp finalRequires, PExp finalEnsures) {
        super(model.getTypeGraph());
        this.model = model;
        this.name = familyName;
        this.exemplarName = exemplarName;
        this.constraint = constraint;
        this.initRequires = initRequires;
        this.initEnsures = initEnsures;
        this.finalRequires = finalRequires;
        this.finalEnsures = finalEnsures;
    }

    public String getName() {
        return name;
    }

    public String getExemplarName() {
        return exemplarName;
    }

    public PExp getConstraint() {
        return constraint;
    }

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

    @Override public MTType toMath() {
        return model;
    }

    @Override public String toString() {
        return name;
    }

    @Override public boolean equals(Object o) {
        boolean result = (o instanceof PTFamily);

        if ( result ) {
            PTFamily oAsPTFamily = (PTFamily) o;

            result =
                    (model.equals(oAsPTFamily.model))
                            && (name.equals(oAsPTFamily.name))
                            && (exemplarName.equals(oAsPTFamily.exemplarName))
                            && (constraint.equals(oAsPTFamily.constraint))
                            && (initRequires.equals(oAsPTFamily.initRequires))
                            && (initEnsures.equals(oAsPTFamily.initEnsures))
                            && (finalRequires.equals(oAsPTFamily.finalRequires))
                            && (finalEnsures.equals(oAsPTFamily.finalEnsures));
        }
        return result;
    }
}
