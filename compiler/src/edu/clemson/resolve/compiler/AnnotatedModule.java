package edu.clemson.resolve.compiler;

import edu.clemson.resolve.RESOLVECompiler;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.model.VCOutputFile;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import edu.clemson.resolve.semantics.DumbMathClssftnHandler;
import edu.clemson.resolve.semantics.MathClassification;
import edu.clemson.resolve.semantics.ModuleIdentifier;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import edu.clemson.resolve.semantics.programtype.ProgType;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    /**
     * For each {@link edu.clemson.resolve.parser.ResolveParser.MathExpContext} and
     * {@link edu.clemson.resolve.parser.ResolveParser.ProgExpContext}, this map keeps a pointer to its corresponding
     * AST (represented by {@link PExp}).
     */
    public ParseTreeProperty<PExp> mathASTs = new ParseTreeProperty<>();

    public final Set<ModuleIdentifier> uses = new LinkedHashSet<>();

    //use a map for more efficiency when checking whether a module references
    //an external impl
    public final Map<String, ModuleIdentifier> externalUses = new HashMap<>();

    /**
     * Think of the {@code uses} set (declared above) as refs useful for coming up with module orderings, etc. Think
     * of these then as the refs the symboltable will see. We don't want implementations of facilities showing up
     * in this set.
     */
    public final Set<ModuleIdentifier> semanticallyRelevantUses = new LinkedHashSet<>();

    private final String fileName;
    private final Token name;
    private final ParseTree root;
    private VCOutputFile vcs = null;
    public boolean hasErrors;
    private final ModuleIdentifier identifier;

    public AnnotatedModule(@NotNull ParseTree root,
                           @NotNull Token name,
                           @NotNull String fileName,
                           boolean hasErrors,
                           @NotNull Set<ModuleIdentifier> uses) {
        this.hasErrors = hasErrors;
        this.root = root;
        this.name = name;
        this.fileName = fileName;
        this.uses.addAll(uses);

        this.identifier = new ModuleIdentifier(name, new File(fileName));
    }

    /**
     * returns the fully qualified name of this module/file. These qualified names are relative to RESOLVEROOT
     * and RESOLVEPATH.
     * @return
     */
    public String getFullyQualifiedName() {
        return name.getText();
    }

    public void setVCs(@Nullable VCOutputFile vco) {
        this.vcs = vco;
    }

    @NotNull
    public PExp getMathExpASTFor(@NotNull DumbMathClssftnHandler g, @Nullable ParserRuleContext ctx) {
        PExp result = mathASTs.get(ctx);
        return result != null ? result : g.getTrueExp();
    }

    @NotNull
    public Token getNameToken() {
        return name;
    }

    @NotNull
    public String getFilePath() {
        return fileName;
    }

    @NotNull
    public ParseTree getRoot() {
        return root;
    }

    @Nullable
    public VCOutputFile getVCOutput() {
        return vcs;
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