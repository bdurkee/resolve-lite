package resolvelite.compiler.tree;

import org.antlr.runtime.tree.ParseTree;
import org.antlr.v4.runtime.misc.NotNull;
import resolvelite.parsing.ResolveBaseListener;
import resolvelite.parsing.ResolveParser;

/**
 * Fills in the contents of an {@link ImportCollection} by visiting the
 * various nodes in an {@link ParseTree} that reference other modules.
 */
public class ImportListener extends ResolveBaseListener {

    public static final ImportListener INSTANCE = new ImportListener();

    private final ImportCollection.ImportCollectionBuilder builder =
            new ImportCollection.ImportCollectionBuilder();

    @NotNull
    public ImportCollection getImports() {
        return builder.build();
    }

    //Todo: override facilities, enhancements, etc when they get added to the
    //grammar.

    @Override
    public void exitImportList(
            @NotNull ResolveParser.ImportListContext ctx) {
        builder.imports(ImportCollection.ImportType.EXPLICIT, ctx.Identifier());
    }
}
