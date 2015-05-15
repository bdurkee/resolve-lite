package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.semantics.MTType;
import org.resolvelite.semantics.programtype.PTFamily;
import org.resolvelite.semantics.TypeGraph;

/**
 * Describes a "Type Family" introduction as would be found in a concept
 * file.
 */
public class ProgTypeModelSymbol extends ProgTypeSymbol {

    private final MathSymbol exemplar;

    public ProgTypeModelSymbol(TypeGraph g, String name, MTType modelType,
            PTFamily programType, MathSymbol exemplar, ParseTree definingTree,
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
