package edu.clemson.resolve.semantics.symbol;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import edu.clemson.resolve.semantics.ModuleIdentifier;
import edu.clemson.resolve.semantics.UnexpectedSymbolException;
import edu.clemson.resolve.semantics.programtype.ProgType;

import java.util.Map;

/** A wrapper for a 'parameter-like symbol' such as an {@link OperationSymbol},
 *  {@link ProgParameterSymbol}, or {@link MathClssftnWrappingSymbol} that happens to be
 *  functioning as a formal parameter for a module when declared.
 */
public class ModuleParameterSymbol extends Symbol {

    private final Symbol wrappedParamSymbol;

    public ModuleParameterSymbol(@NotNull Symbol symbol, @NotNull String name,
                                 @Nullable ParserRuleContext definingTree,
                                @NotNull ModuleIdentifier moduleIdentifier) {
        super(name, definingTree, moduleIdentifier);
        this.wrappedParamSymbol = symbol;
    }

    public ModuleParameterSymbol(ProgParameterSymbol p) {
        this(p, p.getName(), p.definingTree, p.getModuleIdentifier());
    }

    public ModuleParameterSymbol(MathClssftnWrappingSymbol p) {
        this(p, p.getName(), p.definingTree, p.getModuleIdentifier());
    }

    /** Returns the program type; will be {@code null} in the case where we
     *  wrap a {@code MathSymbol} (arising from a defn passed to facility).
     */
    @Nullable public ProgType getProgramType() {
        ProgType progType = null;
        if (wrappedParamSymbol instanceof OperationSymbol) {
            progType = ((OperationSymbol) wrappedParamSymbol).getReturnType();
        }
        else if (wrappedParamSymbol instanceof ProgParameterSymbol) {
            progType = ((ProgParameterSymbol) wrappedParamSymbol).getDeclaredType();
        }
        return progType;
    }

    @Override public boolean isModuleTypeParameter() {
        boolean result = (wrappedParamSymbol instanceof ProgParameterSymbol);
        if (result) {
            result = ((ProgParameterSymbol)wrappedParamSymbol).getMode() ==
                    ProgParameterSymbol.ParameterMode.TYPE;
        }
        return result;
    }

    @Nullable public PExp asPSymbol() {
        if (!(wrappedParamSymbol instanceof ProgParameterSymbol)) return null;
        return new PSymbol.PSymbolBuilder(
                ((ProgParameterSymbol) wrappedParamSymbol).asPSymbol()).build();
    }

    @Override public boolean isModuleOperationParameter() {
        return wrappedParamSymbol instanceof OperationSymbol;
    }

    @NotNull public Symbol getWrappedParamSymbol() {
        return wrappedParamSymbol;
    }

    /** Handle these toXXXX methods strategically, meaning only those that a
     *  conceivably module parameterizable {@link Symbol} might need.
     */
    @NotNull @Override public MathClssftnWrappingSymbol toMathSymbol()
            throws UnexpectedSymbolException {
        return wrappedParamSymbol.toMathSymbol();
    }

    @NotNull @Override public ProgVariableSymbol toProgVariableSymbol()
            throws UnexpectedSymbolException {
        return wrappedParamSymbol.toProgVariableSymbol();
    }

    @NotNull @Override public ProgParameterSymbol toProgParameterSymbol()
            throws UnexpectedSymbolException {
        return wrappedParamSymbol.toProgParameterSymbol();
    }

    @NotNull @Override public ProgTypeSymbol toProgTypeSymbol()
            throws UnexpectedSymbolException {
        return wrappedParamSymbol.toProgTypeSymbol();
    }

    @NotNull @Override public String getSymbolDescription() {
        return "a module parameter symbol";
    }

    @NotNull @Override public Symbol instantiateGenerics(
            @NotNull Map<String, ProgType> genericInstantiations,
            @Nullable FacilitySymbol instantiatingFacility) {
        return wrappedParamSymbol.instantiateGenerics(
                genericInstantiations, instantiatingFacility);
    }
}
