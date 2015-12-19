package org.rsrg.semantics.symbol;

import edu.clemson.resolve.proving.absyn.PSymbol;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.rsrg.semantics.programtype.PTGeneric;
import org.rsrg.semantics.programtype.PTType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ProgParameterSymbol extends Symbol {

    public static enum ParameterMode {
        ALTERS {
            @Override public ParameterMode[] getValidImplementationModes() {
                return new ParameterMode[] { ALTERS, CLEARS };
            }
        },
        UPDATES {
            @Override public ParameterMode[] getValidImplementationModes() {
                return new ParameterMode[] { UPDATES, CLEARS, RESTORES,
                        PRESERVES };
            }
        },
        REPLACES {
            @Override public ParameterMode[] getValidImplementationModes() {
                return new ParameterMode[] { REPLACES, CLEARS };
            }
        },
        CLEARS {
            @Override public ParameterMode[] getValidImplementationModes() {
                return new ParameterMode[] { CLEARS };
            }
        },
        RESTORES {
            @Override public ParameterMode[] getValidImplementationModes() {
                return new ParameterMode[] { RESTORES, PRESERVES };
            }
        },
        PRESERVES {
            @Override public ParameterMode[] getValidImplementationModes() {
                return new ParameterMode[] { PRESERVES };
            }
        },
        EVALUATES {
            @Override public ParameterMode[] getValidImplementationModes() {
                return new ParameterMode[]{EVALUATES};
            }
        },
        TYPE {
            @Override public ParameterMode[] getValidImplementationModes() {
                return new ParameterMode[] { TYPE };
            }
        };
        public boolean canBeImplementedWith(ParameterMode o) {
            return contains(getValidImplementationModes(), o);
        }

        private static boolean contains(Object[] os, Object o) {
            boolean result = false;
            int i = 0;
            int osLength = os.length;
            while (!result && i < osLength) {
                result = os[i].equals(o);
                i++;
            }
            return result;
        }

        public abstract ParameterMode[] getValidImplementationModes();
    }

    public static Map<String, ParameterMode> getModeMapping() {
        Map<String, ParameterMode> result = new HashMap<>();
        ParameterMode[] modes = ParameterMode.values();
        for (ParameterMode mode : modes) {
            result.put(mode.toString().toLowerCase(), mode);
        }
        return Collections.unmodifiableMap(result);
    }

    @NotNull private final ParameterMode mode;
    @NotNull private final PTType declaredType;
    @NotNull private final TypeGraph typeGraph;

    @NotNull private final MathSymbol mathSymbolAlterEgo;
    @NotNull private final ProgVariableSymbol progVariableAlterEgo;

    public ProgParameterSymbol(@NotNull TypeGraph g, @NotNull String name,
                               @NotNull ParameterMode mode,
                               @NotNull PTType type,
                               @NotNull ParserRuleContext definingTree,
                               @NotNull ModuleIdentifier moduleIdentifier) {
        super(name, definingTree, moduleIdentifier);
        this.typeGraph = g;
        this.declaredType = type;
        this.mode = mode;

        MTType typeValue = null;
        if (mode == ParameterMode.TYPE) {
            typeValue = new PTGeneric(type.getTypeGraph(), name).toMath();
        }

        //TODO: Probably need to recajigger this to correctly account for any
        //      generics in the defining context
        this.mathSymbolAlterEgo =
                new MathSymbol(g, name, Quantification.NONE, type.toMath(),
                        typeValue, definingTree, moduleIdentifier);

        this.progVariableAlterEgo =
                new ProgVariableSymbol(getName(), getDefiningTree(),
                        declaredType, getModuleIdentifier());
    }

    @NotNull public PTType getDeclaredType() {
        return declaredType;
    }

    @NotNull public ParameterMode getMode() {
        return mode;
    }

    @NotNull @Override public MathSymbol toMathSymbol() {
        return mathSymbolAlterEgo;
    }

    @NotNull @Override public ProgVariableSymbol toProgVariableSymbol() {
        return progVariableAlterEgo;
    }

    @NotNull @Override public ProgParameterSymbol toProgParameterSymbol() {
        return this;
    }

    @NotNull @Override public ProgTypeSymbol toProgTypeSymbol()
            throws UnexpectedSymbolException {
        ProgTypeSymbol result;

        if (!mode.equals(ParameterMode.TYPE)) {
            //This will throw an appropriate error
            result = super.toProgTypeSymbol();
        }
        else {
            result =
                    new ProgTypeSymbol(typeGraph, getName(), new PTGeneric(
                            typeGraph, getName()), new MTNamed(typeGraph, getName()),
                            getDefiningTree(), getModuleIdentifier());
        }
        return result;
    }

    @NotNull public PSymbol asPSymbol() {
        return new PSymbol.PSymbolBuilder(getName())
                .progType(declaredType)
                .mathType(declaredType.toMath()).build();
    }

    @NotNull @Override public String getSymbolDescription() {
        return "a parameter";
    }

    @NotNull @Override public Symbol instantiateGenerics(
            @NotNull Map<String, PTType> genericInstantiations,
            @Nullable FacilitySymbol instantiatingFacility) {

        return new ProgParameterSymbol(typeGraph, getName(), mode,
                declaredType.instantiateGenerics(genericInstantiations,
                        instantiatingFacility), getDefiningTree(),
                getModuleIdentifier());
    }

    @Override public String toString() {
        return "<" + mode.toString().toLowerCase() + ">" + getName();
    }
}
