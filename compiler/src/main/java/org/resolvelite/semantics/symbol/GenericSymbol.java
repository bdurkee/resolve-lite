package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.semantics.MTNamed;
import org.resolvelite.semantics.MTType;
import org.resolvelite.semantics.Quantification;
import org.resolvelite.semantics.VariableReplacingVisitor;
import org.resolvelite.semantics.programtype.PTElement;
import org.resolvelite.semantics.programtype.PTGeneric;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.Map;

public class GenericSymbol extends ProgTypeSymbol {

    private final MathSymbol mathSymbolAlterEgo;

    public GenericSymbol(TypeGraph g, String name, ParseTree definingTree,
            String moduleID) {
        super(g, name, new PTGeneric(g, name), new MTNamed(g, name),
                definingTree, moduleID);

        MTType typeValue =
                new PTGeneric(getProgramType().getTypeGraph(), getName())
                        .toMath();
        mathSymbolAlterEgo =
                new MathSymbol(g, name, Quantification.NONE, type.toMath(),
                        typeValue, definingTree, moduleID);
    }

    @Override public String getSymbolDescription() {
        return "a generic";
    }

    @Override public GenericSymbol toGenericSymbol() {
        return this;
    }

    @Override public MathSymbol toMathSymbol() {
        return mathSymbolAlterEgo;
    }

    //Todo: As long is this guy is a subclass, this should no
    //longer be necessary
    /*   @Override public ProgTypeSymbol toProgTypeSymbol() {
           return new ProgTypeSymbol(g, getName(), new PTGeneric(g, getName()),
                   new MTNamed(g, getName()), getDefiningTree(), getModuleID());
       }*/

    @Override public ProgTypeSymbol instantiateGenerics(
            Map<String, PTType> genericInstantiations,
            FacilitySymbol instantiatingFacility) {

        Map<String, MTType> genericMathematicalInstantiations =
                Symbol.buildMathTypeGenerics(genericInstantiations);

        VariableReplacingVisitor typeSubstitutor =
                new VariableReplacingVisitor(genericMathematicalInstantiations);
        modelType.accept(typeSubstitutor);

        return new ProgTypeSymbol(modelType.getTypeGraph(),
                genericInstantiations.get(getName()).toString(),
                getProgramType(), typeSubstitutor.getFinalExpression(),
                getDefiningTree(), getModuleID());
    }
}
