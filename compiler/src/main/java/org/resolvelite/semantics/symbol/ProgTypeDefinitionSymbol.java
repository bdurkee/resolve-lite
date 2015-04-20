package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.semantics.MTType;
import org.resolvelite.semantics.programtype.PTFamily;
import org.resolvelite.typereasoning.TypeGraph;

public class ProgTypeDefinitionSymbol extends ProgTypeSymbol {

    private MathSymbol exemplar;

    public ProgTypeDefinitionSymbol(TypeGraph g, String name,
            ParseTree definingTree, String moduleID) {
        super(g, name, definingTree, moduleID);
    }

    public void setExemplarSymbol(MathSymbol exemplar) {
        this.exemplar = exemplar;
    }

    public MathSymbol getExemplar() {
        return this.exemplar;
    }

    @Override public PTFamily getProgramType() {
        return (PTFamily) super.getProgramType();
    }

    @Override public ProgTypeDefinitionSymbol toProgTypeDefinitionSymbol() {
        return this;
    }

}
