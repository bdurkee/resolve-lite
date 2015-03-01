package resolvelite.typeandpopulate;

import org.antlr.v4.runtime.misc.NotNull;
import resolvelite.ResolveCompiler;
import resolvelite.compiler.AnnotatedParseTree;
import resolvelite.parsing.ResolveBaseListener;
import resolvelite.compiler.AnnotatedParseTree.TreeAnnotatingBuilder;
import resolvelite.parsing.ResolveParser;
import resolvelite.typeandpopulate.entry.SymbolTableEntry;
import resolvelite.typereasoning.TypeGraph;

import java.util.Deque;
import java.util.LinkedList;

public class PopulatingListener extends ResolveBaseListener {

    private MathSymbolTableBuilder builder;

    /**
     * <p>Any quantification-introducing syntactic node (like, e.g., a
     * QuantExp), introduces a level to this stack to reflect the quantification
     * that should be applied to named variables as they are encountered.  Note
     * that this may change as the children of the node are processed--for
     * example, MathVarDecs found in the declaration portion of a QuantExp
     * should have quantification (universal or existential) applied, while
     * those found in the body of the QuantExp should have no quantification
     * (unless there is an embedded QuantExp).  In this case, QuantExp should
     * <em>not</em> remove its layer, but rather change it to
     * MathSymbolTableEntry.None.</p>
     *
     * <p>This stack is never empty, but rather the bottom layer is always
     * MathSymbolTableEntry.None.</p>
     */
    private Deque<SymbolTableEntry.Quantification> activeQuantifications =
            new LinkedList<SymbolTableEntry.Quantification>();

    private final ResolveCompiler compiler;
    private final TypeGraph typeGraph;

    public TypeGraph getTypeGraph() {
        return typeGraph;
    }

    public PopulatingListener(ResolveCompiler rc,
                              TreeAnnotatingBuilder annotations,
                              MathSymbolTableBuilder builder) {
        this.activeQuantifications.push(SymbolTableEntry.Quantification.NONE);
        this.typeGraph = builder.getTypeGraph();
        this.builder = builder;
        this.compiler = rc;
    }

    @Override
    public void enterModule(@NotNull ResolveParser.ModuleContext ctx) {

    }
}
