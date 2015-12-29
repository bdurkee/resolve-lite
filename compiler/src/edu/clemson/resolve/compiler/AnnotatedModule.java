package edu.clemson.resolve.compiler;

import edu.clemson.resolve.parser.ResolveParser;
import edu.clemson.resolve.proving.absyn.PExp;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.ModuleIdentifier;
import org.rsrg.semantics.TypeGraph;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.rsrg.semantics.MTType;
import org.rsrg.semantics.programtype.PTType;

import java.util.*;

/** Represents a collection of information to be associated with a top level
 *  {@link edu.clemson.resolve.parser.ResolveParser.ModuleDeclContext}.
 *  <p>
 *  We use this approach over {@code returns} clauses in the grammar to help
 *  us keep our grammar as general as possible.</p>
 */
public class AnnotatedModule {

    public ParseTreeProperty<MTType> mathTypes = new ParseTreeProperty<>();
    public ParseTreeProperty<MTType> mathTypeValues = new ParseTreeProperty<>();
    public ParseTreeProperty<PTType> progTypes = new ParseTreeProperty<>();
    public ParseTreeProperty<PTType> progTypeValues = new ParseTreeProperty<>();

    public ParseTreeProperty<PExp> mathPExps = new ParseTreeProperty<>();


    public final Set<ModuleIdentifier> uses = new LinkedHashSet<>();

    //use a map for more efficiency when checking whether a module references
    //an external impl
    public final Map<String, ModuleIdentifier> externalUses = new HashMap<>();

    /** Think of the {@code uses} set as refs useful for coming up with module
     *  orderings, etc. Think of these strings the refs the symboltable will see.
     *  We don't want implementations of facilities showing up in this set.
     */
    //UPDATE: Can't really remember why the hell I made a separate set for this.
    //need to think about it some more, likely in trying to delete I'll remember,
    //but let me try to remember why I thought it was necessary (several months ago)
    //UPDATE2: Ok, now i remember, like the comment says, we need to consider
            //facility impls when coming up with module compile ordering, but
            //we don't want to include them in the set of available things the
            //symboltable machinery will search through when going through imports. I think
            //that was it anyways.
    public final Set<ModuleIdentifier> semanticallyRelevantUses =
            new LinkedHashSet<>();

    @NotNull private final String fileName;
    @NotNull private final Token name;
    @NotNull private final ParseTree root;
    public boolean hasErrors;

    public AnnotatedModule(@NotNull ParseTree root, @NotNull Token name) {
        this(root, name, "", false);
    }

    public AnnotatedModule(@NotNull ParseTree root, @NotNull Token name,
                           @NotNull String fileName) {
        this(root, name, fileName, false);
    }

    public AnnotatedModule(@NotNull ParseTree root, @NotNull Token name,
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

    @NotNull public PExp getPExpFor(@NotNull TypeGraph g,
                                    @NotNull ParserRuleContext ctx) {
        PExp result = mathPExps.get(ctx);
        return result != null ? result : g.getTrueExp();
    }

    @NotNull public Token getNameToken() {
        return name;
    }

    @NotNull public String getFileName() {
        return fileName;
    }

    @NotNull public ParseTree getRoot() {
        return root;
    }

    /** If we represent a module that implements some root conceptual specification,
     *  this method returns its {@link ModuleIdentifier}; {@code null} otherwise.
     *
     *  @return a module identifier
     */
    @Nullable public ModuleIdentifier getParentConceptIdentifier() {
        ModuleIdentifier result = null;
        if (root.getChild(0) instanceof
                ResolveParser.ConceptImplModuleDeclContext) {
            result = new ModuleIdentifier(
                    ((ResolveParser.ConceptImplModuleDeclContext) root
                            .getChild(0)).concept);
        }
        else if (root.getChild(0) instanceof
                ResolveParser.ConceptExtImplModuleDeclContext) {
            result = new ModuleIdentifier(
                    ((ResolveParser.ConceptExtImplModuleDeclContext) root
                            .getChild(0)).concept);
        }
        return result;
    }

    @Override public int hashCode() {
        return name.hashCode();
    }

    @Override public boolean equals(Object o) {
        boolean result = (o instanceof AnnotatedModule);
        if ( result ) {
            result = this.name.getText()
                    .equals(((AnnotatedModule) o).name.getText());
        }
        return result;
    }

    @Override public String toString() {
        return name.getText();
    }

}