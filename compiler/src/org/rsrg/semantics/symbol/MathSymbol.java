package org.rsrg.semantics.symbol;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.TypeGraph;
import org.antlr.v4.runtime.ParserRuleContext;
import org.rsrg.semantics.*;
import org.rsrg.semantics.programtype.PTType;

import java.util.*;

public class MathSymbol extends Symbol {

    /**
     * Backing fields for {@link MTType}s representing the type and type value
     * of this math symbol, respectively.
     */
    @NotNull private final MTType type;
    @Nullable private final MTType typeValue;
    @NotNull private final Quantification quantification;

    @NotNull private final Map<String, MTType> genericsInDefiningContext =
            new HashMap<>();

    /**
     * Math symbols that represent definitions can take parameters, which may
     * contain implicit type parameters that cause the definition's true type
     * to change based on the type of arguments that end up actually passed.
     * These parameters are represented in this map, with the key giving the
     * name of the type parameter (which will then behave as a normal, bound,
     * named type within the definition's type) and the value giving the type
     * bounds of the parameter.
     */
    @NotNull private final Map<String, MTType> schematicTypes = new HashMap<>();

    public MathSymbol(@NotNull TypeGraph g, @NotNull String name,
                      @NotNull Quantification q,
                      @Nullable Map<String, MTType> schematicTypes,
                      @NotNull MTType type, @Nullable MTType typeValue,
                      @Nullable ParserRuleContext definingTree,
                      @NotNull ModuleIdentifier moduleIdentifier) {
        super(name, definingTree, moduleIdentifier);

        this.type = type;
        this.quantification = q;
        if (schematicTypes != null) {
            this.schematicTypes.putAll(schematicTypes);
        }
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

    public MathSymbol(@NotNull TypeGraph g, @NotNull String name,
                      @NotNull MTType type, @Nullable MTType typeValue,
                      @Nullable ParserRuleContext definingTree,
                      @NotNull ModuleIdentifier moduleIdentifier) {
        this(g, name, Quantification.NONE, new HashMap<String, MTType>(),
                type, typeValue, definingTree, moduleIdentifier);
    }

    public MathSymbol(@NotNull TypeGraph g, @NotNull String name,
                      @NotNull Quantification q, @NotNull MTType type,
                      @Nullable MTType typeValue,
                      @Nullable ParserRuleContext definingTree,
                      @NotNull ModuleIdentifier moduleIdentifier) {
        this(g, name, q, new HashMap<String, MTType>(),
                type, typeValue, definingTree, moduleIdentifier);
    }

    public MathSymbol(@NotNull TypeGraph g, @NotNull String name,
                      @NotNull Map<String, MTType> schematicTypes,
                      @NotNull MTType type, @Nullable MTType typeValue,
                      @Nullable ParserRuleContext definingTree,
                      @NotNull ModuleIdentifier moduleIdentifier) {
        this(g, name, Quantification.NONE, schematicTypes, type, typeValue,
                definingTree, moduleIdentifier);
    }

    @NotNull public MTType getType() {
        return type;
    }

    /**
     * Returns the type value of this {@code MathSymbol}. We say {@code NotNull}
     * since this throws a catchable exception otherwise; so if this returns
     * it will indeed be not null.
     *
     * @return the type value
     * @throws SymbolNotOfKindTypeException if the type value is {@code null}
     */
    @NotNull public MTType getTypeValue() throws SymbolNotOfKindTypeException {
        if ( typeValue == null ) throw new SymbolNotOfKindTypeException();
        return typeValue;
    }

    @NotNull public Quantification getQuantification() {
        return quantification;
    }

    @NotNull @Override public String getSymbolDescription() {
        return "a math symbol";
    }

    @NotNull public static List<MTType> getParameterTypes(
            @NotNull MTFunction source) {
        return expandAsNeeded(source.getDomain());
    }

    @NotNull private static List<MTType> expandAsNeeded(@NotNull MTType t) {
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

    @NotNull @Override public MathSymbol toMathSymbol() {
        return this;
    }

    @NotNull @Override public String toString() {
        return getModuleIdentifier() + "::" + getName() + "\t\t" + quantification
                + "\t\tof type: " + type + "\t\t defines type: " + typeValue;
    }

    @NotNull @Override public Symbol instantiateGenerics(
            @NotNull Map<String, PTType> genericInstantiations,
            @Nullable FacilitySymbol instantiatingFacility) {

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
                new HashMap<String, MTType>(genericsInDefiningContext);
        newGenericsInDefiningContext.keySet().removeAll(
                genericInstantiations.keySet());

        return new MathSymbol(type.getTypeGraph(), getName(),
                getQuantification(), typeSubstitutor.getFinalExpression(),
                instantiatedTypeValue, getDefiningTree(), getModuleIdentifier());
    }

}
