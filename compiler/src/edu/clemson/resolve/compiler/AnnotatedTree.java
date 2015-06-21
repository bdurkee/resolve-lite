package edu.clemson.resolve.compiler;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

public class AnnotatedTree {

    private final String name, fileName;
    private final ParseTree root;
    public boolean hasErrors;
    public ImportCollection imports;

    public AnnotatedTree(@NotNull ParseTree root, @NotNull String name,
                         String fileName, boolean hasErrors) {
        this.hasErrors = hasErrors;
        this.root = root;
        this.name = name;
        this.fileName = fileName;
        //if we have syntactic errors, better not risk processing imports with
        //our tree (as it usually will result in a flurry of npe's).
        if ( !hasErrors ) {
            ImportListener l = new ImportListener();
            ParseTreeWalker.DEFAULT.walk(l, root);
            this.imports = l.getImports();
        }
        else {
            this.imports = new ImportCollection();
        }
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