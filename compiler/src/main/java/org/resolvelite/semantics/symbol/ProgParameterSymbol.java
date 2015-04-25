package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.semantics.programtype.PTInvalid;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.typereasoning.TypeGraph;

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
                return new ParameterMode[] { EVALUATES };
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
    private PTType declaredType;
    private final TypeGraph typeGraph;

    private final MathSymbol mathSymbolAlterEgo;

    //private final Prog progVariable;

    public ProgParameterSymbol(TypeGraph g, String name, ParameterMode mode,
            PTType type, ParseTree definingTree, String moduleID) {
        super(name, definingTree, moduleID);
        this.typeGraph = g;
        this.mode = mode;
        this.declaredType = type;
        this.mathSymbolAlterEgo =
                new MathSymbol(g, name, Quantification.NONE, type.toMath(),
                        null, definingTree, moduleID);
    }

    @Override public MathSymbol toMathSymbol() {
        if ( declaredType == null ) {
            throw new IllegalStateException("no math type set yet");
        }
        this.mathSymbolAlterEgo.setTypes(declaredType.toMath(), null);
        return mathSymbolAlterEgo;
    }

    public void setProgramType(PTType t) {
        this.declaredType = t;
    }

    public ParameterMode getMode() {
        return mode;
    }

    @Override public ProgParameterSymbol toProgParameterSymbol() {
        return this;
    }

    @Override public String getEntryTypeDescription() {
        return "a parameter";
    }

    @Override public String toString() {
        return "<" + mode.toString().toLowerCase() + ">" + getName();
    }
}
