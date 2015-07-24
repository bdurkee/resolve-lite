package edu.clemson.resolve.compiler;

import edu.clemson.resolve.proving.absyn.PExp;
import org.antlr.v4.runtime.Token;
import org.rsrg.semantics.TypeGraph;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.rsrg.semantics.MTType;
import org.rsrg.semantics.programtype.PTType;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class AnnotatedTree {

    public ParseTreeProperty<MTType> mathTypes = new ParseTreeProperty<>();
    public ParseTreeProperty<MTType> mathTypeValues = new ParseTreeProperty<>();
    public ParseTreeProperty<PTType> progTypes = new ParseTreeProperty<>();
    public ParseTreeProperty<PTType> progTypeValues = new ParseTreeProperty<>();

    //Yes, this also exists in SymbolTable.java, but we keep a pointer here for
    //convenience too.
    public ParseTreeProperty<PExp> mathPExps = new ParseTreeProperty<>();
    public final Set<UsesRef> uses = new LinkedHashSet<>();
    //use a map for more efficiency when checking whether a module references
    //an external impl
    public final Map<String, UsesRef> externalUses = new HashMap<>();

    private final String name, fileName;
    private final ParseTree root;
    public boolean hasErrors;

    public AnnotatedTree(@NotNull ParseTree root, @NotNull String name,
                         String fileName, boolean hasErrors) {
        this.hasErrors = hasErrors;
        this.root = root;
        this.name = name;
        this.fileName = fileName;
        //if we have syntactic errors, better not risk processing imports with
        //our tree (as it usually will result in a flurry of npe's).
        if ( !hasErrors ) {
            UsesListener l = new UsesListener(this);
            ParseTreeWalker.DEFAULT.walk(l, root);
        }
    }

    public PExp getPExpFor(TypeGraph g, ParserRuleContext ctx) {
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
        public Token location;
        public String name;
        public UsesRef(Token ref) {
            this(ref, ref == null ? null : ref.getText());
        }
        public UsesRef(Token location, String name) {
            if (name == null) {
                throw new IllegalArgumentException("null uses ref");
            }
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