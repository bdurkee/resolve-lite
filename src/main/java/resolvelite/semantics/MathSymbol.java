package resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Nullable;

public class MathSymbol extends SymbolWithScope implements TypedSymbol {

    protected ParserRuleContext tree;
    protected BaseSymbol.Quantification quantification;
    protected MTType mathType, mathTypeValue;

    public MathSymbol(String name) {
        this(name, null);
    }

    public MathSymbol(String name, @Nullable ParserRuleContext tree) {
        this(name, BaseSymbol.Quantification.NONE, tree);
    }

    public MathSymbol(String name, BaseSymbol.Quantification q,
            @Nullable ParserRuleContext tree) {
        super(name);
        this.quantification = q;
        this.tree = tree;
    }

    @Override
    public MathSymbol toMathSymbol() {
        return this;
    }

    public MTType getMathType() {
        return mathType;
    }

    public MTType getMathTypeValue() {
        return mathTypeValue;
    }

    public void setMathType(MTType type) {
        this.mathType = type;
    }

    public void setMathTypeValue(MTType typeValue) {
        this.mathTypeValue = typeValue;
    }
}
