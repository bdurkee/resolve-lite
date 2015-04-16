package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.ParserRuleContext;
import org.resolvelite.semantics.MathType;

public class MathSymbol extends BaseSymbol {

    private final MathType type;
    private final MathType typeValue;
    public ParserRuleContext definingCtx;
    public MathSymbol(String name, MathType type, MathType typeValue,
                      ParserRuleContext definingCtx,
                      String rootModuleID) {
        super(name, rootModuleID);
        this.definingCtx = definingCtx;
        this.type = type;
        this.typeValue = typeValue;
    }


}
