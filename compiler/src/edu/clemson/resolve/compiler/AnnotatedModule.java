package edu.clemson.resolve.compiler;

import edu.clemson.resolve.proving.absyn.PExp;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import edu.clemson.resolve.semantics.DumbTypeGraph;
import edu.clemson.resolve.semantics.MathClassification;
import edu.clemson.resolve.semantics.ModuleIdentifier;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import edu.clemson.resolve.semantics.programtype.ProgType;

import java.util.*;

/**
 * Represents a collection of information to be associated with a top level
 * {@link edu.clemson.resolve.parser.ResolveParser.ModuleDeclContext}.
 * <p>
 * We use this approach over {@code returns} clauses in the grammar to help us keep our grammar as general
 * as possible.</p>
 *
 * @author dtwelch
 */
public class AnnotatedModule {

    public ParseTreeProperty<MathClassification> mathClssftns = new ParseTreeProperty<>();
    public ParseTreeProperty<ProgType> progTypes = new ParseTreeProperty<>();
    public ParseTreeProperty<PExp> mathASTs = new ParseTreeProperty<>();

    public final Set<ModuleIdentifier> uses = new LinkedHashSet<>();

    //use a map for more efficiency when checking whether a module references
    //an external impl
    public final Map<String, ModuleIdentifier> externalUses = new HashMap<>();

    /**
     * Think of the {@code uses} set as refs useful for coming up with module orderings, etc. Think of these strings
     * the refs the symboltable will see. We don't want implementations of facilities showing up in this set.
     */
    public final Set<ModuleIdentifier> semanticallyRelevantUses = new LinkedHashSet<>();

    private final String fileName;
    private final Token name;
    private final ParseTree root;
    public boolean hasErrors;

    public AnnotatedModule(@NotNull ParseTree root, @NotNull Token name) {
        this(root, name, "", false);
    }

    public AnnotatedModule(@NotNull ParseTree root, @NotNull Token name, @NotNull String fileName) {
        this(root, name, fileName, false);
    }

    public AnnotatedModule(@NotNull ParseTree root,
                           @NotNull Token name,
                           @NotNull String fileName,
                           boolean hasErrors) {
        this.hasErrors = hasErrors;
        this.root = root;
        this.name = name;
        this.fileName = fileName;
        //if we have syntactic errors, better not risk processing imports with
        //our tree (as it usually will result in a flurry of npe's).
        if (!hasErrors) {
            UsesListener l = new UsesListener(this);
            ParseTreeWalker.DEFAULT.walk(l, root);
        }
    }

    @NotNull
    public PExp getMathExpASTFor(@NotNull DumbTypeGraph g,
                                 @NotNull ParserRuleContext ctx) {
        PExp result = mathASTs.get(ctx);
        return result != null ? result : g.getTrueExp();
    }

    @NotNull
    public Token getNameToken() {
        return name;
    }

    @NotNull
    public String getFileName() {
        return fileName;
    }

    @NotNull
    public ParseTree getRoot() {
        return root;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        boolean result = (o instanceof AnnotatedModule);
        if (result) {
            result = this.name.getText().equals(((AnnotatedModule) o).name.getText());
        }
        return result;
    }

    @Override
    public String toString() {
        return name.getText();
    }

}