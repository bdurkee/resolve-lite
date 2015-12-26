package edu.clemson.resolve.analysis;

import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import edu.clemson.resolve.parser.ResolveParser;
import edu.clemson.resolve.parser.ResolveBaseListener;
import edu.clemson.resolve.proving.absyn.PExp;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.xpath.XPath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.programtype.PTType;

/** Uses a combination of listeners and visitors to check for some semantic
 *  errors uncheckable in the grammar and ommited by {@link PopulatingVisitor}.
 *  <p>
 *  To make many of our checks here easier, we make sure to have already built
 *  the ast for exprs in a previous phase.</p>
 */
public class SanityCheckingListener extends ResolveBaseListener {

    private final RESOLVECompiler compiler;
    private final AnnotatedModule tr;

    public SanityCheckingListener(@NotNull RESOLVECompiler compiler,
                                  @NotNull AnnotatedModule tr) {
        this.compiler = compiler;
        this.tr = tr;
    }

    @Override public void exitConceptModuleDecl(
            ResolveParser.ConceptModuleDeclContext ctx) {
        sanityCheckBlockEnds(ctx.name, ctx.closename);
    }

    /*@Override public void exitEnhancementModule(
            ResolveParser.EnhancementModuleContext ctx) {
        sanityCheckBlockEnds(ctx.name, ctx.closename);
    }*/

    @Override public void exitFacilityModuleDecl(
            ResolveParser.FacilityModuleDeclContext ctx) {
        sanityCheckBlockEnds(ctx.name, ctx.closename);
    }

    @Override public void exitConceptImplModuleDecl(
            ResolveParser.ConceptImplModuleDeclContext ctx) {
        sanityCheckBlockEnds(ctx.name, ctx.closename);
    }

    /*@Override public void exitEnhancementImplModule(
            ResolveParser.EnhancementImplModuleContext ctx) {
        sanityCheckBlockEnds(ctx.name, ctx.closename);
    }*/

    @Override public void exitPrecisModuleDecl(
            ResolveParser.PrecisModuleDeclContext ctx) {
        sanityCheckBlockEnds(ctx.name, ctx.closename);
    }

    @Override public void exitProcedureDecl(
            ResolveParser.ProcedureDeclContext ctx) {
        sanityCheckBlockEnds(ctx.name, ctx.closename);
        sanityCheckRecursiveProcKeyword(ctx, ctx.name, ctx.recursive);
    }

    @Override public void exitOperationProcedureDecl(
            ResolveParser.OperationProcedureDeclContext ctx) {
        sanityCheckBlockEnds(ctx.name, ctx.closename);
        sanityCheckRecursiveProcKeyword(ctx, ctx.name, ctx.recursive);
    }

    @Override public void exitAssignStmt(ResolveParser.AssignStmtContext ctx) {
        sanityCheckProgOpTypes(ctx, tr.progTypes.get(ctx.left),
                tr.progTypes.get(ctx.right));
    }

    @Override public void exitSwapStmt(ResolveParser.SwapStmtContext ctx) {
        sanityCheckProgOpTypes(ctx,tr.progTypes.get(ctx.left),
                tr.progTypes.get(ctx.right));
    }

    @Override public void exitRequiresClause(
            ResolveParser.RequiresClauseContext ctx) {
        PExp requires = tr.mathPExps.get(ctx);
        if (requires != null && !requires.getIncomingVariables().isEmpty()) {
            compiler.errMgr.semanticError(
                    ErrorKind.ILLEGAL_INCOMING_REF_IN_REQUIRES, ctx.getStart(),
                    requires.getIncomingVariables(),
                    ctx.mathAssertionExp().getText());
        }
    }

    private void sanityCheckProgOpTypes(@NotNull ParserRuleContext ctx,
                                        @NotNull PTType l, @NotNull PTType r) {
        if (!l.equals(r)) {
            compiler.errMgr.semanticError(ErrorKind.INCOMPATIBLE_OP_TYPES,
                    ctx.getStart(), ctx.getText(), l.toString(), r.toString());
        }
    }

    private void sanityCheckRecursiveProcKeyword(@NotNull ParserRuleContext ctx,
                                                 @NotNull Token name,
                                                 @Nullable Token recursiveToken) {
        boolean hasRecRef = hasRecursiveReferenceInStmts(ctx, name);
        if (recursiveToken == null && hasRecRef) {
            compiler.errMgr.semanticError(
                    ErrorKind.UNLABELED_RECURSIVE_FUNC, name, name.getText(),
                    name.getText());
        }
        else if (recursiveToken != null && !hasRecRef) {
            compiler.errMgr.semanticError(
                    ErrorKind.LABELED_NON_RECURSIVE_FUNC, name, name.getText());
        }
    }

    private void sanityCheckBlockEnds(@NotNull Token topName,
                                      @NotNull Token bottomName) {
        if (!topName.getText().equals(bottomName.getText())) {
            compiler.errMgr.semanticError(
                    ErrorKind.MISMATCHED_BLOCK_END_NAMES, bottomName,
                    topName.getText(), bottomName.getText());
        }
    }

    /** Returns {@code true} if {@code name} appears anywhere in some named
     *  context {@code ctx}.
     *
     *  @param ctx the search context
     *  @param name the name of the (potentially) recursive reference
     *  @return whether or not {@code name} is referenced
     *  recursively in {@code ctx}
     */
    private boolean hasRecursiveReferenceInStmts(@NotNull ParserRuleContext ctx,
                                                 @NotNull Token name) {
        RecursiveCallCheckingListener searcher =
                new RecursiveCallCheckingListener(name.getText());
        ParseTreeWalker.DEFAULT.walk(searcher, ctx);
        return searcher.found;
    }

    protected static class RecursiveCallCheckingListener
            extends
                ResolveBaseListener {
        private final String recursiveCall;
        public boolean found = false;

        public RecursiveCallCheckingListener(@NotNull String call) {
            this.recursiveCall = call;
        }
        @Override public void exitProgParamExp(
                ResolveParser.ProgParamExpContext ctx) {
            String foundName = ctx.progNamedExp().name.getText();

            if (ctx.progNamedExp().qualifier == null &&
                    foundName.equals(recursiveCall)) found = true;
        }
    }
}
