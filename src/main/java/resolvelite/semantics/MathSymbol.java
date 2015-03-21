package resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Nullable;
import resolvelite.typereasoning.TypeGraph;

public class MathSymbol extends SymbolWithScope {

    protected ParserRuleContext tree;
    protected BaseSymbol.Quantification quantification;
    protected MathType mathType, mathTypeValue;
    protected final TypeGraph typeGraph;

    public MathSymbol(TypeGraph g, String name, MathType type,
            MathType typeValue) {
        this(g, name, type, typeValue, null);
    }

    public MathSymbol(TypeGraph g, String name, MathType type,
            MathType typeValue, @Nullable ParserRuleContext tree) {
        this(g, name, type, typeValue, BaseSymbol.Quantification.NONE, tree);
    }

    public MathSymbol(TypeGraph g, String name, MathType type,
            MathType typeValue, BaseSymbol.Quantification q,
            @Nullable ParserRuleContext tree) {
        super(name);
        this.typeGraph = g;
        this.quantification = q;
        this.tree = tree;
        this.mathType = type;
        this.mathType = type;
        if ( typeValue != null ) {
            this.mathTypeValue = typeValue;
        }
        else if ( mathType != null
                && mathType.isKnownToContainOnlyThingsThatAreTypes() ) {
            this.mathTypeValue =
                    new MathTypeProp(typeGraph, type,
                            type.membersKnownToContainOnlyThingsThatAreTypes(),
                            name);
        }
        else {
            this.mathTypeValue = null;
        }
    }

    public BaseSymbol.Quantification getQuantification() {
        return quantification;
    }

    public MathType getMathType() {
        return mathType;
    }

    public MathType getMathTypeValue() throws IllegalStateException {
        if ( mathTypeValue == null ) {
            throw new IllegalStateException();
        }
        return mathTypeValue;
    }
}
