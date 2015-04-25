package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.typereasoning.TypeGraph;

public class ProgRepTypeSymbol extends Symbol {

    //Note: This is null in the case where we represent a standalone
    //representation (a record in a facility for instance)
    protected final ProgTypeDefinitionSymbol definition;

    protected final ParseTree convention, correspondence;
    protected final TypeGraph typeGraph;
    protected PTType representation;

    public ProgRepTypeSymbol(TypeGraph g, String name,
            ParseTree definingElement, String moduleID,
            ProgTypeDefinitionSymbol definition, PTType representation,
            ParseTree convention, ParseTree correspondence) {
        super(name, definingElement, moduleID);

        this.definition = definition;
        this.representation = representation;
        this.convention = convention;
        this.correspondence = correspondence;
        this.typeGraph = g;
    }

    public void setRepresentationType(PTType t) {
        this.representation = t;
    }

    @Override public ProgTypeSymbol toProgTypeSymbol() {
        return new ProgTypeSymbol(typeGraph, getName(), representation,
                (definition == null) ? null : definition.modelType,
                getDefiningTree(), getModuleID());
    }

    @Override public ProgRepTypeSymbol toRepresentationSymbol() {
        return this;
    }

    @Override public String getEntryTypeDescription() {
        return "a program type representation definition";
    }

}
