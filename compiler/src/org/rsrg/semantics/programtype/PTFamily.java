package org.rsrg.semantics.programtype;

import edu.clemson.resolve.proving.absyn.PExp;
import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.MathNamedType;
import org.rsrg.semantics.MathType;
import org.rsrg.semantics.ModuleIdentifier;
import org.rsrg.semantics.symbol.FacilitySymbol;
import org.rsrg.semantics.symbol.Symbol;

import java.util.Map;

public class PTFamily extends PTNamed {

    @NotNull private final MathType model;
    @NotNull private final String name, exemplarName;
    @NotNull private final PExp constraint;

    public PTFamily(@NotNull MathType model, @NotNull String name,
                    @NotNull String exemplarName, @NotNull PExp constraint,
                    @NotNull PExp initEnsures,
                    @NotNull ModuleIdentifier moduleIdentifier) {
        super(model.getTypeGraph(), name, initEnsures, moduleIdentifier);
        this.model = model;
        this.name = name;
        this.exemplarName = exemplarName;
        this.constraint = constraint;
    }

    @NotNull public String getName() {
        return name;
    }

    @NotNull public String getExemplarName() {
        return exemplarName;
    }

    @NotNull public PExp getConstraint() {
        return constraint;
    }

    @NotNull @Override public MathType toMath() {
        return model;
    }

    @Override public String toString() {
        return name;
    }

    @NotNull @Override public ProgType instantiateGenerics(
            @NotNull Map<String, ProgType> genericInstantiations,
            @NotNull FacilitySymbol instantiatingFacility) {

        Map<String, MathType> stringToMathType =
                Symbol.buildMathTypeGenerics(genericInstantiations);

      /*  @SuppressWarnings("unchecked") Map<MathType, MathType> mathTypeToMathType =
                (Map<MathType, MathType>) (Map<?, MathType>) MathNamedType.toMTNamedMap(
                        getTypeGraph(), stringToMathType);

        //Todo: Not currently substituting generics into math expressions..
        MathType newModel =
                myModel.getCopyWithVariablesSubstituted(stringToMathType);

        PExp newConstraint =
                myConstraint.withTypesSubstituted(mathTypeToMathType);

        PExp newInitializationEnsures =
                myInitializationEnsures
                        .withTypesSubstituted(mathTypeToMathType);*/

        return new PTFamily(model, name, exemplarName, constraint, initEnsures,
                getModuleIdentifier());
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
