package org.rsrg.semantics.symbol;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.rsrg.semantics.MTType;
import org.rsrg.semantics.UnexpectedSymbolException;
import org.rsrg.semantics.programtype.PTType;

import java.util.HashMap;
import java.util.Map;

public abstract class Symbol {

    protected final String name, moduleID;
    protected final ParserRuleContext definingTree;

    public Symbol(String name, ParserRuleContext definingTree,
                  String moduleID) {
        this.name = name;
        this.definingTree = definingTree;
        this.moduleID = moduleID;
    }

    public String getModuleID() {
        return moduleID;
    }

    public String getName() {
        return name;
    }

    //Todo: This should really be changed across the board to return
    //"ParserRuleContext" instead, as that gives start and stop info easier
    //(without needing casts)
    public ParserRuleContext getDefiningTree() {
        return definingTree;
    }

    public abstract String getSymbolDescription();

    public MathSymbol toMathSymbol() throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException(this.getSymbolDescription());
    }

    public ProgTypeSymbol toProgTypeSymbol() throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException(this.getSymbolDescription());
    }

    public ProgTypeModelSymbol toProgTypeModelSymbol()
            throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException(this.getSymbolDescription());
    }

    public ProgParameterSymbol toProgParameterSymbol()
            throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException(this.getSymbolDescription());
    }

    public OperationSymbol toOperationSymbol() throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException(this.getSymbolDescription());
    }

    public GenericSymbol toGenericSymbol() throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException(this.getSymbolDescription());
    }

    public FacilitySymbol toFacilitySymbol() throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException(this.getSymbolDescription());
    }

    public ProgReprTypeSymbol toProgReprTypeSymbol()
            throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException(this.getSymbolDescription());
    }

    public ProgVariableSymbol toProgVariableSymbol()
            throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException(this.getSymbolDescription());
    }

    public GlobalMathAssertionSymbol toWrappedGlobalSpecSymbol()
            throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException(this.getSymbolDescription());
    }

    public ProcedureSymbol toProcedureSymbol() {
        throw new UnexpectedSymbolException(this.getSymbolDescription());
    }

    public abstract Symbol instantiateGenerics(
            Map<String, PTType> genericInstantiations,
            FacilitySymbol instantiatingFacility);

    public static Map<String, MTType> buildMathTypeGenerics(
            Map<String, PTType> genericInstantiations) {

        Map<String, MTType> genericMathematicalInstantiations =
                new HashMap<String, MTType>();

        for (Map.Entry<String, PTType> instantiation : genericInstantiations
                .entrySet()) {
            genericMathematicalInstantiations.put(instantiation.getKey(),
                    instantiation.getValue().toMath());
        }
        return genericMathematicalInstantiations;
    }

}
