package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.misc.NotNull;
import org.resolvelite.semantics.Scope;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ParameterSymbol extends BaseSymbol implements TypedSymbol {

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

    @NotNull private final ParameterMode mode;

    public ParameterSymbol(String name, ParameterMode mode,
            Scope enclosingScope, String rootModuleID) {
        super(enclosingScope, name, rootModuleID);
        this.mode = mode;
    }

    @NotNull public ParameterMode getMode() {
        return mode;
    }

    //Todo: this toString sucks. Make it better.
    @Override public String toString() {
        String s = "";
        s =
                scope.getScopeDescription() + ".("
                        + mode.toString().toLowerCase() + ")";
        if ( type != null ) {
            return '<' + s + getName() + "." + type + '>';
        }
        return s + getName();
    }
}
