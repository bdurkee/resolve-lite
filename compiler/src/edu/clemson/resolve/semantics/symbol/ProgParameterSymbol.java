package edu.clemson.resolve.semantics.symbol;

import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.semantics.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.antlr.v4.runtime.ParserRuleContext;
import edu.clemson.resolve.semantics.programtype.ProgGenericType;
import edu.clemson.resolve.semantics.programtype.ProgType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ProgParameterSymbol extends Symbol {

    public static enum ParameterMode {
        ALTERS {
            @Override
            public ParameterMode[] getValidImplementationModes() {
                return new ParameterMode[]{ALTERS, CLEARS};
            }
        },
        UPDATES {
            @Override
            public ParameterMode[] getValidImplementationModes() {
                return new ParameterMode[]{UPDATES, CLEARS, RESTORES,
                        PRESERVES};
            }
        },
        REPLACES {
            @Override
            public ParameterMode[] getValidImplementationModes() {
                return new ParameterMode[]{REPLACES, CLEARS};
            }
        },
        CLEARS {
            @Override
            public ParameterMode[] getValidImplementationModes() {
                return new ParameterMode[]{CLEARS};
            }
        },
        RESTORES {
            @Override
            public ParameterMode[] getValidImplementationModes() {
                return new ParameterMode[]{RESTORES, PRESERVES};
            }
        },
        PRESERVES {
            @Override
            public ParameterMode[] getValidImplementationModes() {
                return new ParameterMode[]{PRESERVES};
            }
        },
        EVALUATES {
            @Override
            public ParameterMode[] getValidImplementationModes() {
                return new ParameterMode[]{EVALUATES};
            }
        },
        TYPE {
            @Override
            public ParameterMode[] getValidImplementationModes() {
                return new ParameterMode[]{TYPE};
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

    private final ParameterMode mode;
    private final ProgType declaredType;
    private final DumbTypeGraph typeGraph;

    private MathClssftnWrappingSymbol mathSymbolAlterEgo;
    private final ProgVariableSymbol progVariableAlterEgo;

    @Nullable
    private String typeQualifier;

    public ProgParameterSymbol(@NotNull DumbTypeGraph g, @NotNull String name,
                               @NotNull ParameterMode mode,
                               @NotNull ProgType type,
                               @Nullable ParserRuleContext definingTree,
                               @NotNull ModuleIdentifier moduleIdentifier) {
        super(name, definingTree, moduleIdentifier);
        this.typeGraph = g;
        this.declaredType = type;
        this.mode = mode;

        this.mathSymbolAlterEgo = null;
        if (mode == ParameterMode.TYPE) {
            this.mathSymbolAlterEgo =
                    new MathClssftnWrappingSymbol(g, name, Quantification.NONE, type.toMath(),
                            definingTree, moduleIdentifier);
        } else {
            int level = type.toMath().getTypeRefDepth();
            this.mathSymbolAlterEgo =
                    new MathClssftnWrappingSymbol(g, name, Quantification.NONE,
                            new MathNamedClassification(g, name, level,
                                    type.toMath()),
                            definingTree, moduleIdentifier);
        }
        this.progVariableAlterEgo =
                new ProgVariableSymbol(getName(), getDefiningTree(),
                        declaredType, getModuleIdentifier());
    }

    public void setTypeQualifierString(String typeQualifier) {
        this.typeQualifier = typeQualifier;
    }

    @Nullable
    public String getTypeQualifier() {
        return typeQualifier;
    }

    @NotNull
    public ProgType getDeclaredType() {
        return declaredType;
    }

    @NotNull
    public ParameterMode getMode() {
        return mode;
    }

    @NotNull
    @Override
    public MathClssftnWrappingSymbol toMathSymbol() {
        return mathSymbolAlterEgo;
    }

    @NotNull
    @Override
    public ProgVariableSymbol toProgVariableSymbol() {
        return progVariableAlterEgo;
    }

    @NotNull
    @Override
    public ProgParameterSymbol toProgParameterSymbol() {
        return this;
    }

    @NotNull
    @Override
    public ProgTypeSymbol toProgTypeSymbol()
            throws UnexpectedSymbolException {
        ProgTypeSymbol result = null;

        if (!mode.equals(ParameterMode.TYPE)) {
            //This will throw an appropriate error
            result = super.toProgTypeSymbol();
        } else {
            result =
                    new ProgTypeSymbol(typeGraph, getName(), new ProgGenericType(
                            typeGraph, getName()),
                            mathSymbolAlterEgo.getClassification(),
                            getDefiningTree(), getModuleIdentifier());
        }
        return result;
    }

    @NotNull
    public PSymbol asPSymbol() {
        return new PSymbol.PSymbolBuilder(getName())
                .progType(declaredType)
                .mathType(declaredType.toMath()).build();
    }

    @NotNull
    @Override
    public String getSymbolDescription() {
        return "a parameter";
    }

    @NotNull
    @Override
    public Symbol instantiateGenerics(
            @NotNull Map<String, ProgType> genericInstantiations,
            @Nullable FacilitySymbol instantiatingFacility) {

        return new ProgParameterSymbol(typeGraph, getName(), mode,
                declaredType.instantiateGenerics(genericInstantiations,
                        instantiatingFacility), getDefiningTree(),
                getModuleIdentifier());
    }

    @Override
    public String toString() {
        return "<" + mode.toString().toLowerCase() + ">" + getName();
    }
}
