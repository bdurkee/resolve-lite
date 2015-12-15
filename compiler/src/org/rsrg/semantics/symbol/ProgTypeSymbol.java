package org.rsrg.semantics.symbol;

import edu.clemson.resolve.proving.absyn.PSymbol;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.rsrg.semantics.programtype.PTType;

import java.util.Map;

public class ProgTypeSymbol extends Symbol {

    @Nullable protected final MTType modelType;
    @NotNull protected final PTType type;
    @NotNull protected final MathSymbol mathTypeAlterEgo;
    @NotNull protected final TypeGraph g;

    public ProgTypeSymbol(@NotNull TypeGraph g, @NotNull String name,
                          @NotNull PTType progType,
                          @Nullable MTType modelType,
                          @Nullable ParserRuleContext definingTree,
                          @NotNull ModuleIdentifier moduleIdentifier) {
        super(name, definingTree, moduleIdentifier);
        this.type = progType;
        this.g = g;
        this.modelType = modelType;
        this.mathTypeAlterEgo =
                new MathSymbol(g, name, Quantification.NONE, g.SSET, modelType,
                        definingTree, moduleIdentifier);
    }

    @NotNull public PTType getProgramType() {
        return type;
    }

    @Nullable public MTType getModelType() {
        return modelType;
    }

    @NotNull @Override public MathSymbol toMathSymbol() {
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
            @NotNull Map<String, PTType> genericInstantiations,
            @Nullable FacilitySymbol instantiatingFacility) {

        Map<String, MTType> genericMathematicalInstantiations =
                Symbol.buildMathTypeGenerics(genericInstantiations);

        VariableReplacingVisitor typeSubstitutor =
                new VariableReplacingVisitor(genericMathematicalInstantiations);
        if (modelType != null) {
            modelType.accept(typeSubstitutor);
        }
        return new ProgTypeSymbol(type.getTypeGraph(), getName(),
                getProgramType(), typeSubstitutor.getFinalExpression(),
                getDefiningTree(), getModuleIdentifier());
    }
}
