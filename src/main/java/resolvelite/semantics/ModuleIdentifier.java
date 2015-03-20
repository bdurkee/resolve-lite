package resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import resolvelite.compiler.tree.ResolveAnnotatedParseTree;
import resolvelite.parsing.ResolveParser;

/**
 * Identifies a particular module unambiguously.
 * <p>
 * <strong>Note:</strong> Currently, we only permit one level of namespace. But
 * ultimately that will probably change (because, for example, at this moment if
 * there were two "Stack_Templates", we couldn't deal with that. A java
 * class-path-like solution seems inevitable. For the moment however, this is
 * just a wrapper around the string name of the module to facilitate changing
 * how we deal with modules later.
 */
public class ModuleIdentifier implements Comparable<ModuleIdentifier> {

    public static final ModuleIdentifier GLOBAL = new ModuleIdentifier();

    private final String myName;
    private final boolean myGlobalFlag;

    private ModuleIdentifier() {
        myName = "GLOBAL";
        myGlobalFlag = true;
    }

    public ModuleIdentifier(ParseTree m) {
        this(getModuleNameFromContext(m));
    }

    public ModuleIdentifier(ResolveAnnotatedParseTree.TreeAnnotatingBuilder m) {
        this(m.name.getText());
    }

    public ModuleIdentifier(Token t) {
        this(t.getText());
    }

    public ModuleIdentifier(String s) {
        myName = s;
        myGlobalFlag = false;
    }

    @Override
    public boolean equals(Object o) {
        boolean result = (o instanceof ModuleIdentifier);

        if ( result ) {
            result = ((ModuleIdentifier) o).myName.equals(myName);
        }
        return result;
    }

    public int hashCode() {
        return myName.hashCode();
    }

    @Override
    public int compareTo(ModuleIdentifier o) {
        return myName.compareTo(o.myName);
    }

    public String toString() {
        return myName;
    }

    public static String getModuleNameFromContext(ParseTree ctx) {
        String result = null;
        //In case the user passes a plain ModuleContext node.
        if ( ctx instanceof ResolveParser.ModuleContext ) {
            ctx = ctx.getChild(0); //specific module-ctxs are zeroth child of
            // of the ModuleContext rule/context.
        }
        if ( ctx instanceof ResolveParser.PrecisModuleContext ) {
            ResolveParser.PrecisModuleContext ctxAsPrecisModule =
                    (ResolveParser.PrecisModuleContext) ctx;
            result = ctxAsPrecisModule.name.getText();
        }
        else if ( ctx instanceof ResolveParser.ConceptModuleContext ) {
            ResolveParser.PrecisModuleContext ctxAsPrecisModule =
                    (ResolveParser.PrecisModuleContext) ctx;
            result = ctxAsPrecisModule.name.getText();
        }
        else {
            throw new IllegalArgumentException("cannot retrieve module name "
                    + "from rule context: " + ctx.getClass());
        }
        return result;
    }

    public String fullyQualifiedRepresentation(String symbol) {
        return myName + "::" + symbol;
    }
}