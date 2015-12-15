package org.rsrg.semantics.symbol;

import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.MTType;
import org.rsrg.semantics.ModuleIdentifier;
import org.rsrg.semantics.SyntacticScope;
import org.rsrg.semantics.UnexpectedSymbolException;
import org.rsrg.semantics.programtype.PTType;

import java.util.HashMap;
import java.util.Map;

public abstract class Symbol {

    @NotNull protected final String name;

    /** Identifies the particular module in which this {@code Symbol} lives. */
    @NotNull protected final ModuleIdentifier moduleIdentifier;

    /**
     * A {@code definingTree} may be {@code null} in the case of a typical
     * symbol (eg: something extending {@code Symbol}). However, the defining
     * tree for ctx that <em>define</em> scopes shouldn't be {@code null},
     * hence the choice of annotation for those. See
     * {@link SyntacticScope#getDefiningTree()}
     */
    @Nullable protected final ParserRuleContext definingTree;

    public Symbol(@NotNull String name, @Nullable ParserRuleContext definingTree,
                  @NotNull ModuleIdentifier moduleIdentifier) {
        this.name = name;
        this.definingTree = definingTree;
        this.moduleIdentifier = moduleIdentifier;
    }

    @NotNull public ModuleIdentifier getModuleIdentifier() {
        return moduleIdentifier;
    }

    @NotNull public String getName() {
        return name;
    }

    @Nullable public ParserRuleContext getDefiningTree() {
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

    //public GenericSymbol toGenericSymbol() throws UnexpectedSymbolException {
    //    throw new UnexpectedSymbolException(this.getSymbolDescription());
    //}

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

    public TheoremSymbol toTheoremSymbol() {
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
