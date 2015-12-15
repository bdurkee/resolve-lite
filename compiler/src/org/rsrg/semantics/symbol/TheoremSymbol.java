package org.rsrg.semantics.symbol;

import edu.clemson.resolve.proving.absyn.PExp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.ModuleIdentifier;
import org.rsrg.semantics.TypeGraph;
import org.antlr.v4.runtime.ParserRuleContext;
import org.rsrg.semantics.Quantification;
import org.rsrg.semantics.programtype.PTType;

import java.util.Map;

/**
 *
 * @author hamptos
 */
public class TheoremSymbol extends Symbol {

    @NotNull private PExp assertion;
    @NotNull private MathSymbol mathSymbolAlterEgo;

    public TheoremSymbol(@NotNull TypeGraph g, @NotNull String name,
                         @NotNull PExp theoremAssertion,
                         @Nullable ParserRuleContext definingTree,
                         @NotNull ModuleIdentifier moduleIdentifier) {
        super(name, definingTree, moduleIdentifier);

        this.assertion = theoremAssertion;
        this.mathSymbolAlterEgo =
                new MathSymbol(g, name, Quantification.NONE, g.BOOLEAN, null
                        , definingTree, moduleIdentifier);
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

    @NotNull @Override public MathSymbol toMathSymbol() {
        return mathSymbolAlterEgo;
    }

    @NotNull @Override public Symbol instantiateGenerics(
            @NotNull Map<String, PTType> genericInstantiations,
            @NotNull FacilitySymbol instantiatingFacility) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
