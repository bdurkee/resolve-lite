package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.tree.ParseTree;

public abstract class Symbol {

    public enum Quantification {
        NONE {

            @Override public String toString() {
                return "None";
            }
        },
        UNIVERSAL {

            @Override public String toString() {
                return "Universal";
            }
        },
        EXISTENTIAL {

            @Override public String toString() {
                return "Existential";
            }
        };
    }

    private final String name, moduleID;
    private final ParseTree definingTree;

    public Symbol(String name, ParseTree definingTree, String moduleID) {
        this.name = name;
        this.definingTree = definingTree;
        this.moduleID = moduleID;
    }

    public String getModuleID() {
        return moduleID;
    }

    public String getName() {
        return name;
    }

    public ParseTree getDefiningTree() {
        return definingTree;
    }

}
