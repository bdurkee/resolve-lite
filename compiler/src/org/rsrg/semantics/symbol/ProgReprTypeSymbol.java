package org.rsrg.semantics.symbol;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import org.rsrg.semantics.TypeGraph;
import org.antlr.v4.runtime.ParserRuleContext;
import org.rsrg.semantics.programtype.PTRepresentation;
import org.rsrg.semantics.programtype.PTType;

import java.util.HashMap;
import java.util.List;
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

    public PSymbol conceptualExemplarAsPSymbol() {
        return new PSymbol.PSymbolBuilder(
                "conc."+representation.getExemplarName())
                .mathType(representation.toMath()).build();
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

    /**
     * Returns a map connecting conceptual elements to their definitions on
     * the rhs of a correspondence equality function. This is as opposed to its
     * original form consisting of conjuncted equals exprs.
     *
     * @return a mapping of conceptual labels to their definitions expressed
     * in terms of concrete implementation-speciic variables.
     */
    public Map<PExp, PExp> getCorrespondenceAsExplicitMapping() {
        Map<PExp, PExp> result = new HashMap<>();
        if (correspondence == null) return result;
        for (PExp e : correspondence.splitIntoConjuncts()) {
            if (e.getSubExpressions().size() == 2 && e instanceof PSymbol &&
                    ((PSymbol)e).getName().equals("=")) {
                List<? extends PExp> subExps = e.getSubExpressions();
                result.put(subExps.get(0), subExps.get(1));
            }
        }
        return result;
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
