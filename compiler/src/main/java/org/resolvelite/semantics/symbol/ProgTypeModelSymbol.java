package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.typereasoning.TypeGraph;

public class ProgTypeModelSymbol extends ProgTypeSymbol {

    private MathSymbol exemplar;

    public ProgTypeModelSymbol(TypeGraph g, String name, MathSymbol exemplar,
            ParseTree definingTree, String moduleID) {
        super(g, name, definingTree, moduleID);
        this.exemplar = exemplar;
    }

    public MathSymbol getExemplar() {
        return exemplar;
    }

    @Override public ProgTypeModelSymbol toProgTypeModelSymbol() {
        return this;
    }

}
