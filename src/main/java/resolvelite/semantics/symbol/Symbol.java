package resolvelite.semantics.symbol;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTree;
import resolvelite.semantics.ModuleIdentifier;
import resolvelite.semantics.Scope;

public class Symbol {

    public enum Quantification {
        NONE {

            @Override
            public String toString() {
                return "None";
            }
        },
        UNIVERSAL {

            @Override
            public String toString() {
                return "Universal";
            }
        },
        EXISTENTIAL {

            @Override
            public String toString() {
                return "Existential";
            }
        }
    }

    @NotNull private final String name;
    @Nullable private final ParseTree definingElement;
    @Nullable private final ModuleIdentifier sourceModuleIdentifier;
    public Scope scope;

    public Symbol(@NotNull String name,
                            @Nullable ParseTree definingElement,
                            @Nullable ModuleIdentifier sourceModule) {
        this.name = name;
        this.definingElement = definingElement;
        this.sourceModuleIdentifier = sourceModule;
    }

    public String getName() {
        return name;
    }

    public ParseTree getDefiningElement() {
        return definingElement;
    }

    public String toString() {
        return "<" + getName() + ":" + sourceModuleIdentifier + ">";
    }
}
