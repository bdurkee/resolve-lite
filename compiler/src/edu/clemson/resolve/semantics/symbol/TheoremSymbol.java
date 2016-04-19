package edu.clemson.resolve.semantics.symbol;

import edu.clemson.resolve.proving.absyn.PExp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import edu.clemson.resolve.semantics.DumbTypeGraph;
import edu.clemson.resolve.semantics.ModuleIdentifier;
import org.antlr.v4.runtime.ParserRuleContext;
import edu.clemson.resolve.semantics.Quantification;
import edu.clemson.resolve.semantics.programtype.ProgType;

import java.util.Map;

/**
 *
 * @author hamptos
 */
public class TheoremSymbol extends Symbol {

    @NotNull private PExp assertion;
    @NotNull private MathClssftnWrappingSymbol mathSymbolAlterEgo;

    public TheoremSymbol(@NotNull DumbTypeGraph g, @NotNull String name,
                         @NotNull PExp theoremAssertion,
                         @Nullable ParserRuleContext definingTree,
                         @NotNull ModuleIdentifier moduleIdentifier) {
        super(name, definingTree, moduleIdentifier);

        this.assertion = theoremAssertion;
        this.mathSymbolAlterEgo =
                new MathClssftnWrappingSymbol(g, name, Quantification.NONE, g.BOOLEAN,
                        definingTree, moduleIdentifier);
    }

    @NotNull public PExp getAssertion() {
        return assertion;
    }

    @NotNull @Override public TheoremSymbol toTheoremSymbol() {
        return this;
    }

    @NotNull @Override public String getSymbolDescription() {
        return "a theorem symbol";
    }

    @NotNull @Override public MathClssftnWrappingSymbol toMathSymbol() {
        return mathSymbolAlterEgo;
    }

    @NotNull @Override public Symbol instantiateGenerics(
            @NotNull Map<String, ProgType> genericInstantiations,
            @Nullable FacilitySymbol instantiatingFacility) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
