package org.rsrg.semantics.symbol;

import edu.clemson.resolve.proving.absyn.PExp;
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

    private PExp assertion;
    private MathSymbol mathSymbolAlterEgo;

    public TheoremSymbol(TypeGraph g, String name, PExp theoremAssertion,
                         ParserRuleContext definingTree,
                         String moduleID) {
        super(name, definingTree, moduleID);

        this.assertion = theoremAssertion;

        this.mathSymbolAlterEgo =
                new MathSymbol(g, name, Quantification.NONE, g.BOOLEAN, null
                        , definingTree, moduleID);
    }

    public PExp getAssertion() {
        return assertion;
    }

    @Override public TheoremSymbol toTheoremSymbol() {
        return this;
    }

    @Override public String getSymbolDescription() {
        return "a theorem symbol";
    }

    @Override public MathSymbol toMathSymbol() {
        return mathSymbolAlterEgo;
    }

    @Override public Symbol instantiateGenerics(
            Map<String, PTType> genericInstantiations,
            FacilitySymbol instantiatingFacility) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
