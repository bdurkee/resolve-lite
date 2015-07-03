package org.rsrg.semantics.symbol;

import edu.clemson.resolve.proving.absyn.PSymbol;
import org.rsrg.semantics.TypeGraph;
import org.antlr.v4.runtime.ParserRuleContext;
import org.rsrg.semantics.MTType;
import org.rsrg.semantics.Quantification;
import org.rsrg.semantics.VariableReplacingVisitor;
import org.rsrg.semantics.programtype.PTType;

import java.util.Map;

public class ProgTypeSymbol extends Symbol {

    protected final MTType modelType;
    protected final PTType type;
    protected final MathSymbol mathTypeAlterEgo;
    protected final TypeGraph g;

    public ProgTypeSymbol(TypeGraph g, String name, PTType progType,
            MTType modelType, ParserRuleContext definingTree, String moduleID) {
        super(name, definingTree, moduleID);
        this.type = progType;
        this.g = g;
        this.modelType = modelType;
        this.mathTypeAlterEgo =
                new MathSymbol(g, name, Quantification.NONE, g.SSET, modelType,
                        definingTree, moduleID);
    }

    public PTType getProgramType() {
        return type;
    }

    public MTType getModelType() {
        return modelType;
    }

    @Override public MathSymbol toMathSymbol() {
        return mathTypeAlterEgo;
    }

    @Override public String toString() {
        return getName();
    }

    public PSymbol asPSymbol() {
        return new PSymbol.PSymbolBuilder(getName()).mathType(getModelType())
                .build();
    }

    @Override public ProgTypeSymbol toProgTypeSymbol() {
        return this;
    }

    @Override public String getSymbolDescription() {
        return "a program type";
    }

    @Override public ProgTypeSymbol instantiateGenerics(
            Map<String, PTType> genericInstantiations,
            FacilitySymbol instantiatingFacility) {

        Map<String, MTType> genericMathematicalInstantiations =
                Symbol.buildMathTypeGenerics(genericInstantiations);

        VariableReplacingVisitor typeSubstitutor =
                new VariableReplacingVisitor(genericMathematicalInstantiations);
        modelType.accept(typeSubstitutor);

        return new ProgTypeSymbol(modelType.getTypeGraph(), getName(),
                getProgramType(), typeSubstitutor.getFinalExpression(),
                getDefiningTree(), getModuleID());
    }
}
