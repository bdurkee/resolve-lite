package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.semantics.*;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.semantics.query.GenericQuery;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MathSymbol extends Symbol {

    private MTType type, typeValue;
    private final Quantification quantification;

    private final Map<String, MTType> myGenericsInDefiningContext =
            new HashMap<>();

    public MathSymbol(TypeGraph g, String name, Quantification q, MTType type,
            MTType typeValue, ParseTree definingTree, String moduleID) {
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
            ParseTree definingTree, String moduleID) {
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

    public MathSymbol deschematize(List<PExp> arguments,
        Scope callingContext)
            throws NoSolutionException {
        if (!(type instanceof MTFunction)) throw NoSolutionException.INSTANCE;

        List<MTType> formalParameterTypes =
                getParameterTypes(((MTFunction) type));

        List<MTType> actualArgumentTypes = arguments.stream()
                .map(PExp::getMathType)
                .collect(Collectors.toList());

        if (formalParameterTypes.size() != actualArgumentTypes.size()) {
            throw NoSolutionException.INSTANCE;
        }

        List<ProgTypeSymbol> callingContextProgramGenerics =
                callingContext.query(GenericQuery.INSTANCE);
        Map<String, MTType> callingContextMathGenerics = new HashMap<>();
        Map<String, MTType> bindingsSoFar = new HashMap<>();

        MathSymbol mathGeneric;
        for (ProgTypeSymbol e : callingContextProgramGenerics) {
            //This is guaranteed not to fail--all program types can be coerced
            //to math types, so the passed location is irrelevant
            mathGeneric = e.toMathSymbol();
            callingContextMathGenerics.put(mathGeneric.getName(),
                    mathGeneric.type);
        }


        MTType newTypeValue = null;
        MTType newType =
                ((MTFunction) type
                        .getCopyWithVariablesSubstituted(bindingsSoFar))
                        .deschematize(arguments);

        return new MathSymbol(type.getTypeGraph(), getName(),
                getQuantification(), newType, newTypeValue, getDefiningTree(),
                getModuleID());
    }

    private static List<MTType> getParameterTypes(MTFunction source) {
        return expandAsNeeded(source.getDomain());
    }

    private static List<MTType> expandAsNeeded(MTType t) {
        List<MTType> result = new ArrayList<>();
        if ( t instanceof MTCartesian ) {
            MTCartesian domainAsMTCartesian = (MTCartesian) t;

            for (int i = 0; i < domainAsMTCartesian.size(); i++) {
                result.add(domainAsMTCartesian.getFactor(i));
            }
        }
        else {
            if ( !t.equals(t.getTypeGraph().VOID) ) {
                result.add(t);
            }
        }
        return result;
    }

    @Override public MathSymbol toMathSymbol() {
        return this;
    }

    @Override public String toString() {
        return getModuleID() + "::" + getName() + "\t\t" + quantification
                + "\t\tof type: " + type + "\t\t defines type: " + typeValue;
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
