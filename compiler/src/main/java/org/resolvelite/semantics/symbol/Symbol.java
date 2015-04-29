package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.semantics.MTType;
import org.resolvelite.semantics.UnexpectedSymbolException;
import org.resolvelite.semantics.programtype.PTInvalid;
import org.resolvelite.semantics.programtype.PTType;

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

    /**
     * Returns {@code true} if all {@link PTType}s and {@link MTType}s
     * referenced in this {@code Symbol} do not point to {@link PTInvalid} or
     * the {@code Invalid} math type (respectively).
     * 
     * @return true if this {@link Symbol} lacks a reference to an
     *         'invalid' type.
     */
    public abstract boolean containsOnlyValidTypes();

    public MathSymbol toMathSymbol() throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException();
    }

    public ProgTypeSymbol toProgTypeSymbol() throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException();
    }

    public ProgTypeModelSymbol toProgTypeModelSymbol()
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

    public ProgReprTypeSymbol toProgReprTypeSymbol()
            throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException();
    }

    public ProgVariableSymbol toProgVariableSymbol()
            throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException();
    }

}
