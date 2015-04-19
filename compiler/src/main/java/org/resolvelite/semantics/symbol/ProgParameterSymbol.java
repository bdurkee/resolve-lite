package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.tree.ParseTree;

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

    public ProgParameterSymbol(String name, ParameterMode mode,
            ParseTree definingTree, String moduleID) {
        super(name, definingTree, moduleID);
        this.mode = mode;
    }

    public ParameterMode getMode() {
        return mode;
    }

    @Override public String getEntryTypeDescription() {
        return "a parameter";
    }

    @Override public String toString() {
        return "<" + mode.toString().toLowerCase() + ">" + getName();
    }
}
