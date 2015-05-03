package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.semantics.programtype.PTInvalid;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.typereasoning.TypeGraph;

public class ProgReprTypeSymbol extends Symbol {

    //Note: This is null in the case where we represent a standalone
    //representation (a record in a facility for instance)
    protected final ProgTypeModelSymbol definition;
    protected final ParseTree convention, correspondence;
    protected final TypeGraph typeGraph;
    protected final PTType representation;

    public ProgReprTypeSymbol(TypeGraph g, String name,
            ParseTree definingElement, String moduleID,
            ProgTypeModelSymbol definition, ProgVariableSymbol repVar,
            PTType representation, ParseTree convention,
            ParseTree correspondence) {
        super(name, definingElement, moduleID);
        this.definition = definition;
        this.representation = representation;
        this.convention = convention;
        this.correspondence = correspondence;
        this.typeGraph = g;
    }

    public ProgTypeModelSymbol getDefinition() {
        return definition;
    }

    @Override public ProgTypeSymbol toProgTypeSymbol() {
        return new ProgTypeSymbol(typeGraph, getName(), representation,
                (definition == null) ? null : definition.modelType,
                getDefiningTree(), getModuleID());
    }

    @Override public ProgReprTypeSymbol toProgReprTypeSymbol() {
        return this;
    }

    @Override public String getEntryTypeDescription() {
        return "a program type representation definition";
    }

    @Override public boolean containsOnlyValidTypes() {
        return !representation.getClass().equals(PTInvalid.class);
    }

    @Override public String toString() {
        return getName();
    }

}
