package resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Nullable;
import resolvelite.typereasoning.TypeGraph;

public class MathSymbol extends SymbolWithScope implements TypedSymbol {

    protected ParserRuleContext tree;
    protected BaseSymbol.Quantification quantification;
    protected MTType mathType, mathTypeValue;
    protected final TypeGraph typeGraph;
    public MathSymbol(TypeGraph g, String name) {
        this(g, name, null);
    }

    public MathSymbol(TypeGraph g, String name,
                      @Nullable ParserRuleContext tree) {
        this(g, name, BaseSymbol.Quantification.NONE, tree);
    }

    public MathSymbol(TypeGraph g, String name, BaseSymbol.Quantification q,
            @Nullable ParserRuleContext tree) {
        super(name);
        this.typeGraph = g;
        this.quantification = q;
        this.tree = tree;
    }

    public BaseSymbol.Quantification getQuantification() {
        return quantification;
    }

    @Override
    public MathSymbol toMathSymbol() {
        return this;
    }

    public MTType getMathType() {
        return mathType;
    }

    public MTType getMathTypeValue() {
        if (mathTypeValue == null) {
            throw new IllegalStateException();
        }
        return mathTypeValue;
    }

    public void setMathTypes(MTType type, MTType typeValue) {
        this.mathType = type;
        if (typeValue != null) {
            this.mathTypeValue = typeValue;
        }
        else if (mathType.isKnownToContainOnlyMTypes()) {
            this.mathTypeValue =
                    new MTProper(typeGraph, type, type
                            .membersKnownToContainOnlyMTypes(), name);
        }
        else {
            this.mathTypeValue = null;
        }
    }
}
