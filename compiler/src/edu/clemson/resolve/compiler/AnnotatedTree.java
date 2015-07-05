package edu.clemson.resolve.compiler;

import edu.clemson.resolve.proving.absyn.PExp;
import org.rsrg.semantics.TypeGraph;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.rsrg.semantics.MTType;
import org.rsrg.semantics.programtype.PTType;

public class AnnotatedTree {

    public ParseTreeProperty<MTType> mathTypes = new ParseTreeProperty<>();
    public ParseTreeProperty<MTType> mathTypeValues = new ParseTreeProperty<>();
    public ParseTreeProperty<PTType> progTypes = new ParseTreeProperty<>();
    public ParseTreeProperty<PTType> progTypeValues = new ParseTreeProperty<>();

    //Yes, this also exists in SymbolTable.java, but we keep a pointer here for
    //convenience too.
    public ParseTreeProperty<PExp> mathPExps = new ParseTreeProperty<>();

    private final String name, fileName;
    private final ParseTree root;
    public boolean hasErrors;
    public ImportCollection imports;

    public AnnotatedTree(boolean noStdUses,
                         @NotNull ParseTree root, @NotNull String name,
                         String fileName, boolean hasErrors) {
        this.hasErrors = hasErrors;
        this.root = root;
        this.name = name;
        this.fileName = fileName;
        //if we have syntactic errors, better not risk processing imports with
        //our tree (as it usually will result in a flurry of npe's).
        if ( !hasErrors ) {
            ImportListener l = new ImportListener(noStdUses);
            ParseTreeWalker.DEFAULT.walk(l, root);
            this.imports = l.getImports();
        }
        else {
            this.imports = new ImportCollection();
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
}