package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.Token;
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

    public abstract String getEntryTypeDescription();

    public MathSymbol toMathSymbol() throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException();
    }

    public ProgTypeSymbol toProgTypeSymbol() throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException();
    }

    public ProgTypeDefinitionSymbol toProgTypeDefinitionSymbol()
            throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException();
    }

    public ProgParameterSymbol toProgParameterSymbol()
            throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException();
    }

    public OperationSymbol toOperationSymbol() throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException();
    }

    public GenericSymbol toGenericSymbol() throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException();
    }

    public FacilitySymbol toFacilitySymbol() throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException();
    }

    public RepSymbol toRepresentationSymbol() throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException();
    }
}
