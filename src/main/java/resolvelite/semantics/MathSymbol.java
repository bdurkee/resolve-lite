package resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Nullable;

public class MathSymbol extends SymbolWithScope implements TypedSymbol {

    protected ParserRuleContext tree;
    protected BaseSymbol.Quantification quantification;
    protected MTType mathType, mathTypeValue;

    public MathSymbol(String name, MTType type, MTType typeValue) {
        this(name, type, typeValue, null);
    }

    public MathSymbol(String name, MTType type, MTType typeValue,
            @Nullable ParserRuleContext tree) {
        this(name, type, typeValue, BaseSymbol.Quantification.NONE, tree);
    }

    public MathSymbol(String name, MTType type, MTType typeValue,
                      BaseSymbol.Quantification q,
                      @Nullable ParserRuleContext tree) {
        super(name);
        this.quantification = q;
        this.tree = tree;
        this.mathType = type;
        this.mathTypeValue = typeValue;
    }

    //@Override
    //public MTType getMathType() {
     //   return mathType;
    //}

   // @Override
   // public void setMathType(MTType type) {
   //     this.mathType = type;
   // }

}
