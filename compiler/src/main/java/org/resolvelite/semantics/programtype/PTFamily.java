package org.resolvelite.semantics.programtype;

import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.semantics.MTType;

public class PTFamily extends PTNamed {

    private final MTType model;
    private final String name, exemplarName;
    private final ResolveParser.ConstraintClauseContext constraint;

    public PTFamily(MTType model, String familyName, String exemplarName,
            ResolveParser.ConstraintClauseContext constraint,
            ResolveParser.RequiresClauseContext initRequires,
            ResolveParser.EnsuresClauseContext initEnsures,
            ResolveParser.RequiresClauseContext finalRequires,
            ResolveParser.EnsuresClauseContext finalEnsures,
            String enclosingModuleID) {
        super(model.getTypeGraph(), familyName, initRequires, initEnsures,
                finalRequires, finalEnsures, enclosingModuleID);
        this.model = model;
        this.name = familyName;
        this.exemplarName = exemplarName;
        this.constraint = constraint;
    }

    public String getName() {
        return name;
    }

    public String getExemplarName() {
        return exemplarName;
    }

    public ResolveParser.ConstraintClauseContext getConstraint() {
        return constraint;
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

            //Todo
            result =
                    (model.equals(oAsPTFamily.model))
                            && (name.equals(oAsPTFamily.name))
                            && (exemplarName.equals(oAsPTFamily.exemplarName));
            /* && (constraint.equals(oAsPTFamily.constraint))
             && (initRequires.equals(oAsPTFamily.initRequires))
             && (initEnsures.equals(oAsPTFamily.initEnsures))
             && (finalRequires.equals(oAsPTFamily.finalRequires))
             && (finalEnsures.equals(oAsPTFamily.finalEnsures));*/
        }
        return result;
    }
}
