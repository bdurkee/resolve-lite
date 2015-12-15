package org.rsrg.semantics.symbol;

import edu.clemson.resolve.proving.absyn.PExp;
import org.antlr.v4.runtime.ParserRuleContext;
import org.rsrg.semantics.programtype.PTType;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OperationSymbol extends Symbol {

    private final PTType returnType;
    private final List<ProgParameterSymbol> parameters = new ArrayList<>();
    private final boolean moduleParameter;

    private final PExp requires, ensures;

    public OperationSymbol(String name, ParserRuleContext definingTree,
                           PExp requires, PExp ensures, PTType type,
                           String moduleID, List<ProgParameterSymbol> params,
                           boolean moduleParameter) {
        super(name, definingTree, moduleID);
        this.parameters.addAll(params);
        this.returnType = type;
        this.moduleParameter = moduleParameter;
        this.requires = requires;
        this.ensures = ensures;
    }

    public PExp getRequires() {
        return requires;
    }

    public PExp getEnsures() {
        return ensures;
    }

    public boolean isModuleParameter() {
        return moduleParameter;
    }

    public List<ProgParameterSymbol> getParameters() {
        return parameters;
    }

    public PTType getReturnType() {
        return returnType;
    }

    @Override public OperationSymbol toOperationSymbol() {
        return this;
    }

    @Override public ProgVariableSymbol toProgVariableSymbol() {
        return new ProgVariableSymbol(name, definingTree, returnType, moduleIdentifier);
    }

    @Override public String getSymbolDescription() {
        return "an operation";
    }

    @Override public String toString() {
        return getName() + ":" + parameters;
    }

    @Override public OperationSymbol instantiateGenerics(
            Map<String, PTType> genericInstantiations,
            FacilitySymbol instantiatingFacility) {

        InstantiationFunction f =
                new InstantiationFunction(genericInstantiations,
                        instantiatingFacility);
        List<ProgParameterSymbol> newParams = parameters.stream()
                .map(f::apply).collect(Collectors.toList());
        return new OperationSymbol(getName(), getDefiningTree(), requires,
                ensures, returnType.instantiateGenerics(genericInstantiations,
                        instantiatingFacility), getModuleIdentifier(), newParams,
                moduleParameter);
    }

    private static class InstantiationFunction
            implements
                Function<ProgParameterSymbol, ProgParameterSymbol> {

        private final Map<String, PTType> genericInstantiations;
        private final FacilitySymbol instantiatingFacility;

        public InstantiationFunction(Map<String, PTType> instantiations,
                FacilitySymbol instantiatingFacility) {
            this.genericInstantiations =
                    new HashMap<String, PTType>(instantiations);
            this.instantiatingFacility = instantiatingFacility;
        }

        @Override public ProgParameterSymbol apply(ProgParameterSymbol input) {
            return (ProgParameterSymbol) input.instantiateGenerics(
                    genericInstantiations, instantiatingFacility);
        }
    }
}
