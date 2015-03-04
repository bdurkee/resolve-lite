package resolvelite.typeandpopulate.entry;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import resolvelite.compiler.ResolveCompiler;
import resolvelite.compiler.ErrorKind;

public abstract class SymbolTableEntry {

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
    private final ResolveCompiler compiler;
    private final String name;
    private final ParseTree definingElement;

    public SymbolTableEntry(ResolveCompiler rc, String name,
                            ParseTree definingElement) {
        this.compiler = rc;
        this.name = name;
        this.definingElement = definingElement;
    }

    public String getName() {
        return name;
    }

    public ParseTree getDefiningElement() {
        return definingElement;
    }

    public MathSymbolEntry toMathSymbolEntry(Token l) {
        compiler.errorManager.toolError(ErrorKind.UNEXPECTED_SYMTAB_ENTRY,
                "a math symbol", getEntryTypeDescription());
        return null;
    }

    public abstract String getEntryTypeDescription();

}
