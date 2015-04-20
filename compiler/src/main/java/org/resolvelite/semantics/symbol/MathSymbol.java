package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.semantics.MTProper;
import org.resolvelite.semantics.MTType;
import org.resolvelite.semantics.SymbolNotOfKindTypeException;
import org.resolvelite.typereasoning.TypeGraph;

public class MathSymbol extends Symbol {

    private MTType type, typeValue;
    private final Quantification quantification;
    private final TypeGraph g;

    public MathSymbol(TypeGraph g, String name, Quantification q,
            ParseTree definingTree, MTType type, MTType typeValue,
            String moduleID) {
        super(name, definingTree, moduleID);
        this.g = g;
        this.quantification = q;
        this.setTypes(type, typeValue);
    }

    public MathSymbol(TypeGraph g, String name, Quantification q,
            ParseTree definingTree, String moduleID) {
        super(name, definingTree, moduleID);
        this.g = g;
        this.quantification = q;
    }

    public MathSymbol(TypeGraph g, String name, ParseTree definingTree,
            String moduleID) {
        this(g, name, Quantification.NONE, definingTree, moduleID);
    }

    public void setTypes(MTType mathType, MTType mathTypeValue) {
        if ( mathType == null ) {
            throw new IllegalArgumentException(
                    "passed math type cannot be null");
        }
        this.type = mathType;
        if ( mathTypeValue != null ) {
            this.typeValue = mathTypeValue;
        }
        else if ( type.isKnownToContainOnlyMTypes() ) {
            this.typeValue =
                    new MTProper(g, type,
                            type.membersKnownToContainOnlyMTypes(), getName());
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

    @Override public String toString() {
        return getName();
    }

    @Override public String getEntryTypeDescription() {
        return "a math symbol";
    }

    @Override public MathSymbol toMathSymbol() {
        return this;
    }
}
