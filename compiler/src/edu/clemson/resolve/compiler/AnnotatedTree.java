package edu.clemson.resolve.compiler;

import edu.clemson.resolve.proving.absyn.PExp;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.TypeGraph;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.rsrg.semantics.MTType;
import org.rsrg.semantics.programtype.PTType;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents a collection of information to be associated with a top level
 * {@link edu.clemson.resolve.parser.ResolveParser.ModuleContext}. We use this
 * approach over {@code returns} clauses in the grammar to help us keep our
 * grammar as general as possible.
 */
public class AnnotatedTree {

    public ParseTreeProperty<MTType> mathTypes = new ParseTreeProperty<>();
    public ParseTreeProperty<MTType> mathTypeValues = new ParseTreeProperty<>();
    public ParseTreeProperty<PTType> progTypes = new ParseTreeProperty<>();
    public ParseTreeProperty<PTType> progTypeValues = new ParseTreeProperty<>();

    public ParseTreeProperty<PExp> mathPExps = new ParseTreeProperty<>();
    public final Set<UsesRef> uses = new LinkedHashSet<>();

    //use a map for more efficiency when checking whether a module references
    //an external impl
    public final Map<String, UsesRef> externalUses = new HashMap<>();

    /**
     * Think of the {@code uses} set as refs useful for coming up with module
     * orderings, etc. Think of these strings the refs the symboltable will see.
     * We don't want implementations of facilities showing up in this set.
     */
    public final Set<String> semanticallyVisibleUses = new LinkedHashSet<>();

    @NotNull private final String name, fileName;
    @NotNull private final ParseTree root;
    public boolean hasErrors;

    public AnnotatedTree(@NotNull ParseTree root, @NotNull String name,
                         @NotNull String fileName, boolean hasErrors) {
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

    @Nullable public PExp getPExpFor(TypeGraph g, ParserRuleContext ctx) {
        PExp result = mathPExps.get(ctx);
        return result != null ? result : g.getTrueExp();
    }

    @NotNull public String getName() {
        return name;
    }

    @NotNull public String getFileName() {
        return fileName;
    }

    @NotNull public ParseTree getRoot() {
        return root;
    }

    @Override public int hashCode() {
        return name.hashCode();
    }

    @Override public boolean equals(Object o) {
        boolean result = (o instanceof AnnotatedTree);
        if ( result ) {
            result = this.name.equals(((AnnotatedTree) o).name);
        }
        return result;
    }

    @Override public String toString() {
        return name;
    }

    public static class UsesRef {
        @NotNull public Token location;
        @NotNull public String name;

        public UsesRef(@NotNull Token ref) {
            this(ref, ref.getText());
        }

        public UsesRef(@NotNull Token location, @NotNull String name) {
            this.location = location;
            this.name = name;
        }

        @Override public int hashCode() {
            return name.hashCode();
        }

        @Override public boolean equals(Object o) {
            boolean result = (o instanceof UsesRef);
            if (result) {
                result = ((UsesRef) o).name.equals(this.name);
            }
            return result;
        }
    }
}