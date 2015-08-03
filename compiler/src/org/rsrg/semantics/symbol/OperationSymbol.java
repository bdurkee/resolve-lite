package org.rsrg.semantics.symbol;

import org.antlr.v4.runtime.ParserRuleContext;
import edu.clemson.resolve.parser.Resolve;
import org.rsrg.semantics.MTType;
import org.rsrg.semantics.programtype.PTType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OperationSymbol
        extends
            Symbol implements ModuleParameterizableSymbol {

    private final PTType returnType;
    private final List<ProgParameterSymbol> parameters = new ArrayList<>();
    private final boolean moduleParameter;

    private final Resolve.RequiresClauseContext requires;
    private final Resolve.EnsuresClauseContext ensures;

    public OperationSymbol(String name, ParserRuleContext definingTree,
                           Resolve.RequiresClauseContext requires,
                           Resolve.EnsuresClauseContext ensures,
                           PTType type,
                           String moduleID,
                           List<ProgParameterSymbol> params,
                           boolean moduleParameter) {
        super(name, definingTree, moduleID);
        this.parameters.addAll(params);
        this.returnType = type;
        this.moduleParameter = moduleParameter;
        this.requires = requires;
        this.ensures = ensures;
    }

    public Resolve.RequiresClauseContext getRequires() {
        return requires;
    }

    public Resolve.EnsuresClauseContext getEnsures() {
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
                        instantiatingFacility), getModuleID(), newParams,
                moduleParameter);
    }

    @Override public MTType getMathType() {
        return getProgramType().toMath();
    }

    @Override public PTType getProgramType() {
        return getReturnType();
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
