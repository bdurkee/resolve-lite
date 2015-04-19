package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.semantics.MTProper;
import org.resolvelite.semantics.MTType;
import org.resolvelite.semantics.SymbolNotOfKindTypeException;
import org.resolvelite.typereasoning.TypeGraph;

public class MathSymbol extends Symbol {

    private final MTType type;
    private final MTType typeValue;
    private final Quantification quantification;

    public MathSymbol(TypeGraph g, String name, Quantification q,
            ParseTree definingTree, MTType type, MTType typeValue,
            String moduleID) {
        super(name, definingTree, moduleID);

        this.type = type;
        this.quantification = q;
        if ( typeValue != null ) {
            this.typeValue = typeValue;
        }
        else if ( type.isKnownToContainOnlyMTypes() ) {
            this.typeValue =
                    new MTProper(g, type,
                            type.membersKnownToContainOnlyMTypes(), name);
        }
        else {
            this.typeValue = null;
        }
    }

    public MTType getType() {
        return type;
    }

    public Quantification getQuantification() {
        return quantification;
    }

    public MTType getTypeValue() throws SymbolNotOfKindTypeException {
        if ( typeValue == null ) throw new SymbolNotOfKindTypeException();
        return typeValue;
    }
}
