package edu.clemson.resolve.semantics.symbol;

import edu.clemson.resolve.semantics.MathClassification;
import edu.clemson.resolve.semantics.ModuleIdentifier;
import edu.clemson.resolve.semantics.Quantification;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import edu.clemson.resolve.semantics.DumbMathClssftnHandler;
import org.antlr.v4.runtime.ParserRuleContext;
import edu.clemson.resolve.semantics.programtype.ProgType;

import java.util.*;

public class MathClssftnWrappingSymbol extends Symbol {

    private MathClassification classification;
    private final DumbMathClssftnHandler g;
    private final Quantification q;

    public MathClssftnWrappingSymbol(@NotNull DumbMathClssftnHandler g, @NotNull String name,
                                     @NotNull MathClassification classification) {
        this(g, name, Quantification.NONE, classification, null,
                ModuleIdentifier.GLOBAL);
    }

    public MathClssftnWrappingSymbol(@NotNull DumbMathClssftnHandler g, @NotNull String name,
                                     @NotNull MathClassification classification,
                                     @Nullable ParserRuleContext definingTree,
                                     @NotNull ModuleIdentifier moduleIdentifier) {
        this(g, name, Quantification.NONE, classification, definingTree,
                moduleIdentifier);
    }

    public MathClssftnWrappingSymbol(@NotNull DumbMathClssftnHandler g, @NotNull String name,
                                     @NotNull Quantification q,
                                     @NotNull MathClassification classification,
                                     @Nullable ParserRuleContext definingTree,
                                     @NotNull ModuleIdentifier moduleIdentifier) {
        super(name, definingTree, moduleIdentifier);
        this.classification = classification;
        this.g = g;
        this.q = q;
    }

    public void setClassification(MathClassification n) {
        this.classification = n;
    }

    public MathClassification getClassification() {
        return classification;
    }

    public Quantification getQuantification() {
        return q;
    }

    @NotNull
    @Override
    public String getSymbolDescription() {
        return "a math symbol";
    }

    @NotNull
    @Override
    public MathClssftnWrappingSymbol toMathSymbol() {
        return this;
    }

    @NotNull
    @Override
    public Symbol instantiateGenerics(
            @NotNull Map<String, ProgType> genericInstantiations,
            @Nullable FacilitySymbol instantiatingFacility) {
        /*for (String schematicType : mySchematicTypes.keySet()) {
            genericInstantiations.remove(schematicType);
        }*/

        Map<String, MathClassification> genericMathematicalInstantiations =
                Symbol.buildMathTypeGenerics(genericInstantiations);

        if (genericInstantiations.isEmpty()) return this;
        MathClassification instEncClssftn =
                classification.enclosingClassification
                        .withVariablesSubstituted(
                                genericMathematicalInstantiations);
        MathClassification instClssftn =
                classification
                        .withVariablesSubstituted(
                                genericMathematicalInstantiations);
        instClssftn.enclosingClassification = instEncClssftn;
        int i;
        i = 0;
     /*   VariableReplacingVisitor typeSubstitutor =
                new VariableReplacingVisitor(genericMathematicalInstantiations);
        type.accept(typeSubstitutor);

        MTType instantiatedTypeValue = null;
        if ( typeValue != null ) {
            VariableReplacingVisitor typeValueSubstitutor =
                    new VariableReplacingVisitor(
                            genericMathematicalInstantiations);
            typeValue.accept(typeValueSubstitutor);
            instantiatedTypeValue = typeValueSubstitutor.getFinalExpression();
        }

        Map<String, MTType> newGenericsInDefiningContext =
                new HashMap<String, MTType>(genericsInDefiningContext);
        newGenericsInDefiningContext.keySet().removeAll(
                genericInstantiations.keySet());
*/
        return new MathClssftnWrappingSymbol(g, getName(),
                getQuantification(), instClssftn, getDefiningTree(),
                getModuleIdentifier());
    }

}
