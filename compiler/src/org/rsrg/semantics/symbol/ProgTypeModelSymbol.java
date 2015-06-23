package org.rsrg.semantics.symbol;

import edu.clemson.resolve.typereasoning.TypeGraph;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.rsrg.semantics.MTType;
import org.rsrg.semantics.programtype.PTFamily;

/**
 * Describes a "Type Family" introduction as would be found in a concept
 * file.
 */
public class ProgTypeModelSymbol extends ProgTypeSymbol {

    private final MathSymbol exemplar;

    public ProgTypeModelSymbol(TypeGraph g, String name, MTType modelType,
            PTFamily programType, MathSymbol exemplar, ParserRuleContext definingTree,
            String moduleID) {
        super(g, name, programType, modelType, definingTree, moduleID);
        this.exemplar = exemplar;
    }

    public MathSymbol getExemplar() {
        return exemplar;
    }

    @Override public PTFamily getProgramType() {
        return (PTFamily) super.getProgramType();
    }

    @Override public ProgTypeModelSymbol toProgTypeModelSymbol() {
        return this;
    }
}
