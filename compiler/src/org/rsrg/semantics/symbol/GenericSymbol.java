package org.rsrg.semantics.symbol;

import edu.clemson.resolve.typereasoning.TypeGraph;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.rsrg.semantics.MTNamed;
import org.rsrg.semantics.MTType;
import org.rsrg.semantics.Quantification;
import org.rsrg.semantics.programtype.PTGeneric;
import org.rsrg.semantics.programtype.PTType;

import java.util.Map;

public class GenericSymbol extends Symbol {

    private final MathSymbol mathSymbolAlterEgo;
    private final TypeGraph g;
    private final PTType type;

    public GenericSymbol(TypeGraph g, PTType type, String name,
            ParserRuleContext definingTree, String moduleID) {
        super(name, definingTree, moduleID);
        this.g = g;
        this.type = type;

        MTType typeValue = new PTGeneric(g, getName()).toMath();
        mathSymbolAlterEgo =
                new MathSymbol(g, name, Quantification.NONE, type.toMath(),
                        typeValue, definingTree, moduleID);
    }

    @Override public String getSymbolDescription() {
        return "a generic";
    }

    @Override public GenericSymbol toGenericSymbol() {
        return this;
    }

    @Override public MathSymbol toMathSymbol() {
        return mathSymbolAlterEgo;
    }

    //Todo: As long is this guy is a subclass, this should no
    //longer be necessary
    @Override public ProgTypeSymbol toProgTypeSymbol() {
        return new ProgTypeSymbol(g, getName(), new PTGeneric(g, getName()),
                new MTNamed(g, getName()), getDefiningTree(), getModuleID());
    }

    @Override public GenericSymbol instantiateGenerics(
            Map<String, PTType> genericInstantiations,
            FacilitySymbol instantiatingFacility) {

        return new GenericSymbol(g, type.instantiateGenerics(
                genericInstantiations, instantiatingFacility), name,
                getDefiningTree(), getModuleID());
    }
}
