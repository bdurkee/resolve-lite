package org.rsrg.semantics.symbol;

import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.programtype.PTType;

import java.util.Map;

/** A wrapper for a 'parameter-like symbol' such as an {@link OperationSymbol},
 *  {@link ProgParameterSymbol}, or {@link MathSymbol} that happens to be
 *  functioning as a module formal parameter when declared.
 */
public class ModuleParameterSymbol extends Symbol {

    @NotNull private final Symbol wrappedParamSymbol;

    public ModuleParameterSymbol(Symbol symbol, String name,
                                 ParserRuleContext definingTree,
                                 String moduleID) {
        super(name, definingTree, moduleID);
        this.wrappedParamSymbol = symbol;
    }

    public ModuleParameterSymbol(ProgParameterSymbol p) {
        this(p, p.getName(), p.definingTree, p.getModuleIdentifier());
    }

    @Override public String getSymbolDescription() {
        return "a module parameter symbol";
    }

    @Override public Symbol instantiateGenerics(
            Map<String, PTType> genericInstantiations,
            FacilitySymbol instantiatingFacility) {
        return null;
    }
}
