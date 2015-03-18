package resolvelite.semantics.symbol;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import resolvelite.semantics.ModuleIdentifier;
import resolvelite.semantics.MTType;
import resolvelite.typereasoning.TypeGraph;

public class MathSymbol extends Symbol {

    private final MTType type;
    private final MTType typeValue;
    private final Quantification quantification;

    public MathSymbol(TypeGraph g, String name, Quantification q,
                      ParseTree definingElement, MTType type, MTType typeValue,
                      ModuleIdentifier sourceModule) {
        super(name, definingElement, sourceModule);
        this.type = type;
        this.typeValue = typeValue;
        this.quantification = q;
    }

    public MathSymbol(TypeGraph g, String name, MTType type,
                      MTType typeValue, ModuleIdentifier sourceModule) {
        this(g, name, Quantification.NONE, null, type, typeValue, sourceModule);
    }


    @NotNull public MTType getType() {
        return type;
    }

    @NotNull public MTType getTypeValue() {
        return typeValue;
    }

    @NotNull public Quantification getQuantification() {
        return quantification;
    }
}
