package org.rsrg.semantics.symbol;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSegments;
import edu.clemson.resolve.proving.absyn.PSymbol;
import org.rsrg.semantics.TypeGraph;
import org.antlr.v4.runtime.ParserRuleContext;
import org.rsrg.semantics.programtype.PTRepresentation;
import org.rsrg.semantics.programtype.PTType;

import java.util.Map;

public class ProgReprTypeSymbol extends Symbol {

    //Note: This is null in the case where we represent a standalone
    //representation (e.g.: a facility module bound record)
    protected final ProgTypeModelSymbol definition;
    protected final PExp convention, correspondence;
    protected final TypeGraph typeGraph;
    protected final PTRepresentation representation;

    public ProgReprTypeSymbol(TypeGraph g, String name,
            ParserRuleContext definingElement, String moduleID,
            ProgTypeModelSymbol definition, PTRepresentation representation,
            PExp convention, PExp correspondence) {
        super(name, definingElement, moduleID);
        this.definition = definition;
        this.representation = representation;
        this.convention = convention;
        this.correspondence = correspondence;
        this.typeGraph = g;
    }

    public PSymbol exemplarAsPSymbol() {
        return new PSymbol.PSymbolBuilder(representation.getExemplarName())
                .mathType(representation.toMath()).build();
    }

    public PSegments conceptualExemplarAsPSymbol() {
        return new PSegments(typeGraph.formConcExp(), exemplarAsPSymbol());
    }

    public PTRepresentation getRepresentationType() {
        return representation;
    }

    public ProgTypeModelSymbol getDefinition() {
        return definition;
    }

    public PExp getConvention() {
        return convention;
    }

    public PExp getCorrespondence() {
        return correspondence;
    }

    @Override public ProgTypeSymbol toProgTypeSymbol() {
        return new ProgTypeSymbol(typeGraph, getName(), representation,
                (definition == null) ? null : definition.modelType,
                getDefiningTree(), getModuleID());
    }

    @Override public ProgReprTypeSymbol toProgReprTypeSymbol() {
        return this;
    }

    @Override public String getSymbolDescription() {
        return "a program type representation definition";
    }

    @Override public String toString() {
        return getName();
    }

    @Override public Symbol instantiateGenerics(
            Map<String, PTType> genericInstantiations,
            FacilitySymbol instantiatingFacility) {

        //Representation is an internal implementation detail of a realization
        //and cannot be accessed through a facility instantiation
        throw new UnsupportedOperationException("Cannot instantiate "
                + this.getClass());
    }

}
