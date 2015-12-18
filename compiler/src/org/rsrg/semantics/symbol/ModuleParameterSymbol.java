package org.rsrg.semantics.symbol;

import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.ModuleIdentifier;
import org.rsrg.semantics.UnexpectedSymbolException;
import org.rsrg.semantics.programtype.PTType;

import java.util.Map;

/** A wrapper for a 'parameter-like symbol' such as an {@link OperationSymbol},
 *  {@link ProgParameterSymbol}, or {@link MathSymbol} that happens to be
 *  functioning as a module formal parameter when declared.
 */
public class ModuleParameterSymbol extends Symbol {

    @NotNull private final Symbol wrappedParamSymbol;

    public ModuleParameterSymbol(@NotNull Symbol symbol, String name,
                                 ParserRuleContext definingTree,
                                 ModuleIdentifier moduleIdentifier) {
        super(name, definingTree, moduleIdentifier);
        this.wrappedParamSymbol = symbol;
    }

    public ModuleParameterSymbol(ProgParameterSymbol p) {
        this(p, p.getName(), p.definingTree, p.getModuleIdentifier());
    }

    /** Handle this toXXXX methods strategically, meaning only the ones that
     *  a conceivably module parameterizable {@link Symbol} might need.
     */


    @NotNull @Override public ProgVariableSymbol toProgVariableSymbol()
            throws UnexpectedSymbolException {
        return wrappedParamSymbol.toProgVariableSymbol();
    }

    @NotNull @Override public ProgParameterSymbol toProgParameterSymbol()
            throws UnexpectedSymbolException {
        return wrappedParamSymbol.toProgParameterSymbol();
    }

    @NotNull @Override public String getSymbolDescription() {
        return "a module parameter symbol";
    }

    @NotNull @Override public Symbol instantiateGenerics(
            @NotNull Map<String, PTType> genericInstantiations,
            @Nullable FacilitySymbol instantiatingFacility) {
        return wrappedParamSymbol.instantiateGenerics(
                genericInstantiations, instantiatingFacility);
    }
}
