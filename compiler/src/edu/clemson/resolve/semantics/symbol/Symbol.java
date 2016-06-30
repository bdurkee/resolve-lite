package edu.clemson.resolve.semantics.symbol;

import edu.clemson.resolve.semantics.MathClssftn;
import edu.clemson.resolve.semantics.ModuleIdentifier;
import edu.clemson.resolve.semantics.SyntacticScope;
import edu.clemson.resolve.semantics.UnexpectedSymbolException;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import edu.clemson.resolve.semantics.programtype.ProgType;

import java.util.HashMap;
import java.util.Map;

public abstract class Symbol {

    @NotNull
    protected final String name;

    /**
     * Identifies the particular module in which this {@code Symbol} lives.
     */
    @NotNull
    protected final ModuleIdentifier moduleIdentifier;

    /**
     * The parse tree context this symbol was derived from. Note that it can
     * be {@code null}. Note that contexts that <em>define</em> scopes keep
     * track of this as well.
     *
     * @see {@link SyntacticScope#getDefiningTree()}
     */
    @Nullable
    protected final ParserRuleContext definingTree;

    public Symbol(@NotNull String name,
                  @Nullable ParserRuleContext definingTree,
                  @NotNull ModuleIdentifier moduleIdentifier) {
        this.name = name;
        this.definingTree = definingTree;
        this.moduleIdentifier = moduleIdentifier;
    }

    @NotNull
    public ModuleIdentifier getModuleIdentifier() {
        return moduleIdentifier;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @Nullable
    public ParserRuleContext getDefiningTree() {
        return definingTree;
    }

    public boolean isModuleOperationParameter() {
        return false;
    }

    public boolean isModuleTypeParameter() {
        return false;
    }

    @NotNull
    public abstract String getSymbolDescription();

    @NotNull
    public MathClssftnWrappingSymbol toMathSymbol()
            throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException(this.getSymbolDescription());
    }

    @NotNull
    public ProgTypeSymbol toProgTypeSymbol()
            throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException(this.getSymbolDescription());
    }

    @NotNull
    public TypeModelSymbol toTypeModelSymbol()
            throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException(this.getSymbolDescription());
    }

    @NotNull
    public ProgParameterSymbol toProgParameterSymbol()
            throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException(this.getSymbolDescription());
    }

    @NotNull
    public OperationSymbol toOperationSymbol()
            throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException(this.getSymbolDescription());
    }

    @NotNull
    public FacilitySymbol toFacilitySymbol()
            throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException(this.getSymbolDescription());
    }

    @NotNull
    public ProgReprTypeSymbol toProgReprTypeSymbol()
            throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException(this.getSymbolDescription());
    }

    @NotNull
    public ProgVariableSymbol toProgVariableSymbol()
            throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException(this.getSymbolDescription());
    }

    @NotNull
    public GlobalMathAssertionSymbol toWrappedGlobalSpecSymbol()
            throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException(this.getSymbolDescription());
    }

    @NotNull
    public ProcedureSymbol toProcedureSymbol()
            throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException(this.getSymbolDescription());
    }

    @NotNull
    public TheoremSymbol toTheoremSymbol()
            throws UnexpectedSymbolException {
        throw new UnexpectedSymbolException(this.getSymbolDescription());
    }

    @NotNull
    public abstract Symbol instantiateGenerics(
            @NotNull Map<String, ProgType> genericInstantiations,
            @Nullable FacilitySymbol instantiatingFacility);

    @NotNull
    public static Map<String, MathClssftn> buildMathTypeGenerics(
            @NotNull Map<String, ProgType> genericInstantiations) {

        Map<String, MathClssftn> genericMathematicalInstantiations = new HashMap<>();

        for (Map.Entry<String, ProgType> instantiation : genericInstantiations
                .entrySet()) {
            genericMathematicalInstantiations.put(instantiation.getKey(),
                    instantiation.getValue().toMath());
        }
        return genericMathematicalInstantiations;
    }

}
