package resolvelite.compiler.tree;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.File;

public abstract class AnnotatedParseTree {

    @NotNull protected ImportCollection imports;
    @NotNull protected final ParseTree root;
    public boolean hasErrors;
    protected File file;
    protected Token name;
    protected String fileName;

    public AnnotatedParseTree(@NotNull ParseTree root,
            @NotNull String treeFileName) {
        this.root = root;
        this.fileName = treeFileName;
    }

    @NotNull
    public ImportCollection getImports() {
        return imports;
    }

    @NotNull
    public Token getName() {
        return name;
    }

    @NotNull
    public ParseTree getRoot() {
        return root;
    }

    @NotNull
    public boolean hasErrors() {
        return hasErrors;
    }

}
