package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.semantics.UnexpectedSymbolException;

public abstract class Symbol {

    public enum Quantification {
        NONE {

            @Override public String toString() {
                return "None";
            }
        },
        UNIVERSAL {

            @Override public String toString() {
                return "Universal";
            }
        },
        EXISTENTIAL {

            @Override public String toString() {
                return "Existential";
            }
        };
    }

    private final String name, moduleID;
    private final ParseTree definingTree;

    public Symbol(String name, ParseTree definingTree, String moduleID) {
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

    public ParseTree getDefiningTree() {
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

}
