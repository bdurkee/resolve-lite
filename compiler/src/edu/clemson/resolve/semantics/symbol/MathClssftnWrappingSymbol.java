package edu.clemson.resolve.semantics.symbol;

import edu.clemson.resolve.semantics.MathClssftn;
import edu.clemson.resolve.semantics.ModuleIdentifier;
import edu.clemson.resolve.semantics.Quantification;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import edu.clemson.resolve.semantics.DumbMathClssftnHandler;
import org.antlr.v4.runtime.ParserRuleContext;
import edu.clemson.resolve.semantics.programtype.ProgType;

import java.util.*;

public class MathClssftnWrappingSymbol extends Symbol {

    private MathClssftn classification;
    private final DumbMathClssftnHandler g;
    private final Quantification q;

    /**
     * An operator is "chainable" if it's a relational operator that can be written in the following
     * form x OP y OP z where OP returns a boolean (B). This can then be (internally) interpreted as x OP y and y OP z.
     */
    private final boolean chainable;

    public MathClssftnWrappingSymbol(@NotNull DumbMathClssftnHandler g, @NotNull String name,
                                     @NotNull MathClssftn classification) {
        this(g, name, Quantification.NONE, classification, null, false, ModuleIdentifier.GLOBAL);
    }

    public MathClssftnWrappingSymbol(@NotNull DumbMathClssftnHandler g, @NotNull String name,
                                     @NotNull MathClssftn classification, boolean chainableOperator) {
        this(g, name, Quantification.NONE, classification, null, chainableOperator, ModuleIdentifier.GLOBAL);
    }

    public MathClssftnWrappingSymbol(@NotNull DumbMathClssftnHandler g, @NotNull String name,
                                     @NotNull MathClssftn classification,
                                     @Nullable ParserRuleContext definingTree,
                                     @NotNull ModuleIdentifier moduleIdentifier) {
        this(g, name, Quantification.NONE, classification, definingTree, false, moduleIdentifier);
    }

    public MathClssftnWrappingSymbol(@NotNull DumbMathClssftnHandler g, @NotNull String name,
                                     @NotNull Quantification q,
                                     @NotNull MathClssftn classification,
                                     @Nullable ParserRuleContext definingTree,
                                     @NotNull ModuleIdentifier moduleIdentifier) {
        this(g, name, q, classification, definingTree, false, moduleIdentifier);
    }

    public MathClssftnWrappingSymbol(@NotNull DumbMathClssftnHandler g, @NotNull String name,
                                     @NotNull Quantification q,
                                     @NotNull MathClssftn classification,
                                     @Nullable ParserRuleContext definingTree,
                                     boolean chainableOperator,
                                     @NotNull ModuleIdentifier moduleIdentifier) {
        super(name, definingTree, moduleIdentifier);
        this.classification = classification;
        this.chainable = chainableOperator;
        this.g = g;
        this.q = q;
    }

    public void setClassification(MathClssftn n) {
        this.classification = n;
    }

    public MathClssftn getClassification() {
        return classification;
    }

    public Quantification getQuantification() {
        return q;
    }

    public boolean isChainable() {
        return chainable;
    }

    @NotNull
    @Override
    public String getSymbolDescription() {
        return "a mathFor symbol";
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

        Map<String, MathClssftn> genericMathematicalInstantiations =
                Symbol.buildMathTypeGenerics(genericInstantiations);

        if (genericInstantiations.isEmpty()) return this;
        MathClssftn instEncClssftn =
                classification.enclosingClassification
                        .withVariablesSubstituted(
                                genericMathematicalInstantiations);
        MathClssftn instClssftn =
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
                getQuantification(), instClssftn, getDefiningTree(), chainable,
                getModuleIdentifier());
    }

}
