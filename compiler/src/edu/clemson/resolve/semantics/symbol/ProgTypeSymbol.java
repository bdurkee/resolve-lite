package edu.clemson.resolve.semantics.symbol;

import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.semantics.DumbTypeGraph;
import edu.clemson.resolve.semantics.MathClassification;
import edu.clemson.resolve.semantics.ModuleIdentifier;
import edu.clemson.resolve.semantics.Quantification;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.antlr.v4.runtime.ParserRuleContext;
import edu.clemson.resolve.semantics.programtype.ProgType;

import java.util.Map;

public class ProgTypeSymbol extends Symbol {

    @NotNull protected final MathClassification modelType;
    @NotNull protected final ProgType type;
    @NotNull protected final MathClssftnWrappingSymbol mathTypeAlterEgo;
    @NotNull protected final DumbTypeGraph g;

    public ProgTypeSymbol(@NotNull DumbTypeGraph g, @NotNull String name,
                          @NotNull ProgType progType,
                          @Nullable MathClassification modelType,
                          @Nullable ParserRuleContext definingTree,
                          @NotNull ModuleIdentifier moduleIdentifier) {
        super(name, definingTree, moduleIdentifier);
        this.type = progType;
        this.g = g;
        this.modelType = modelType == null ? g.INVALID : modelType;
        this.mathTypeAlterEgo =
                new MathClssftnWrappingSymbol(g, name, Quantification.NONE, modelType,
                        definingTree, moduleIdentifier);
    }

    @NotNull public ProgType getProgramType() {
        return type;
    }

    @NotNull public MathClassification getModelType() {
        return modelType;
    }

    @NotNull @Override public MathClssftnWrappingSymbol toMathSymbol() {
        return mathTypeAlterEgo;
    }

    @NotNull @Override public String toString() {
        return getName();
    }

    @NotNull public PSymbol asPSymbol() {
        return new PSymbol.PSymbolBuilder(getName()).mathType(getModelType())
                .build();
    }

    @NotNull @Override public ProgTypeSymbol toProgTypeSymbol() {
        return this;
    }

    @NotNull @Override public String getSymbolDescription() {
        return "a program type";
    }

    @NotNull @Override public ProgTypeSymbol instantiateGenerics(
            @NotNull Map<String, ProgType> genericInstantiations,
            @Nullable FacilitySymbol instantiatingFacility) {

        Map<String, MathClassification> genericMathematicalInstantiations =
                Symbol.buildMathTypeGenerics(genericInstantiations);
        if (genericMathematicalInstantiations.isEmpty()) return this;
        MathClassification instModelEncClssftn =
                modelType.enclosingClassification
                        .withVariablesSubstituted(
                                genericMathematicalInstantiations);
        MathClassification instModelClssftn = modelType
                .withVariablesSubstituted(
                        genericMathematicalInstantiations);
        instModelClssftn.enclosingClassification = instModelEncClssftn;
        /*VariableReplacingVisitor typeSubstitutor =
                new VariableReplacingVisitor(genericMathematicalInstantiations);
        if (modelType != null) {
            modelType.accept(typeSubstitutor);
        }*/
        return new ProgTypeSymbol(type.getTypeGraph(), getName(),
                getProgramType(), instModelClssftn,
                getDefiningTree(), getModuleIdentifier());
    }
}
