package org.rsrg.semantics.programtype;

import edu.clemson.resolve.proving.absyn.PExp;
import org.rsrg.semantics.MTNamed;
import org.rsrg.semantics.MTType;
import org.rsrg.semantics.symbol.FacilitySymbol;
import org.rsrg.semantics.symbol.ProgTypeModelSymbol;
import org.rsrg.semantics.symbol.Symbol;

import java.util.Map;

public class PTFamily extends PTNamed {

    private final MTType model;
    private final String name, exemplarName;
    private final PExp constraint;

    public PTFamily(MTType model, String name, String exemplarName,
                         PExp constraint, PExp initEnsures, String enclosingModuleID) {
        super(model.getTypeGraph(), name, initEnsures, enclosingModuleID);
        this.model = model;
        this.name = name;
        this.exemplarName = exemplarName;
        this.constraint = constraint;
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

    @Override public MTType toMath() {
        return model;
    }

    @Override public String toString() {
        return name;
    }

    @Override public PTType instantiateGenerics(
            Map<String, PTType> genericInstantiations,
            FacilitySymbol instantiatingFacility) {

        Map<String, MTType> stringToMathType =
                Symbol.buildMathTypeGenerics(genericInstantiations);

        @SuppressWarnings("unchecked") Map<MTType, MTType> mathTypeToMathType =
                (Map<MTType, MTType>) (Map<?, MTType>) MTNamed.toMTNamedMap(
                        getTypeGraph(), stringToMathType);

        //Todo: Not currently substituting generics into math expressions..
        /*MTType newModel =
                myModel.getCopyWithVariablesSubstituted(stringToMathType);

        PExp newConstraint =
                myConstraint.withTypesSubstituted(mathTypeToMathType);

        PExp newInitializationEnsures =
                myInitializationEnsures
                        .withTypesSubstituted(mathTypeToMathType);*/

        return new PTFamily(model, name, exemplarName, constraint, initEnsures,
                getEnclosingModuleID());
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
             && (initEnsures.equals(oAsPTFamily.initEnsures))*/
        }
        return result;
    }
}
