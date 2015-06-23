package org.rsrg.semantics.symbol;

import edu.clemson.resolve.typereasoning.TypeGraph;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.rsrg.semantics.*;
import org.rsrg.semantics.programtype.PTType;

import java.util.HashMap;
import java.util.Map;

public class MathSymbol extends Symbol {

    private MTType type, typeValue;
    private final Quantification quantification;

    private final Map<String, MTType> myGenericsInDefiningContext =
            new HashMap<>();

    public MathSymbol(TypeGraph g, String name, Quantification q, MTType type,
                      MTType typeValue, ParserRuleContext definingTree,
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
                            type.membersKnownToContainOnlyMTypes(), getName());
        }
        else {
            this.typeValue = null;
        }
    }

    public MathSymbol(TypeGraph g, String name, MTType type, MTType typeValue,
                      ParserRuleContext definingTree, String moduleID) {
        this(g, name, Quantification.NONE, type, typeValue, definingTree,
                moduleID);
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

    public void setMathType(MTType t) {
        this.type = t;
    }

    public void setMathTypeValue(MTType t) {
        this.typeValue = t;
    }

    @Override public String getSymbolDescription() {
        return "a math symbol";
    }

    @Override public Symbol instantiateGenerics(
            Map<String, PTType> genericInstantiations,
            FacilitySymbol instantiatingFacility) {

        //Any type that appears in our list of schematic types shadows any
        //possible reference to a generic type
        genericInstantiations =
                new HashMap<String, PTType>(genericInstantiations);
        /*    for (String schematicType : mySchematicTypes.keySet()) {
                genericInstantiations.remove(schematicType);
            }*/

        Map<String, MTType> genericMathematicalInstantiations =
                Symbol.buildMathTypeGenerics(genericInstantiations);

        VariableReplacingVisitor typeSubstitutor =
                new VariableReplacingVisitor(genericMathematicalInstantiations);
        type.accept(typeSubstitutor);

        MTType instantiatedTypeValue = null;
        if ( typeValue != null ) {
            VariableReplacingVisitor typeValueSubstitutor =
                    new VariableReplacingVisitor(
                            genericMathematicalInstantiations);
            typeValue.accept(typeValueSubstitutor);
            instantiatedTypeValue = typeValueSubstitutor.getFinalExpression();
        }

        Map<String, MTType> newGenericsInDefiningContext =
                new HashMap<String, MTType>(myGenericsInDefiningContext);
        newGenericsInDefiningContext.keySet().removeAll(
                genericInstantiations.keySet());

        return new MathSymbol(type.getTypeGraph(), getName(),
                getQuantification(), typeSubstitutor.getFinalExpression(),
                instantiatedTypeValue, getDefiningTree(), getModuleID());
    }

}
