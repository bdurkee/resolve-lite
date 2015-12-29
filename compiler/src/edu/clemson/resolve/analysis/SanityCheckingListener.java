package edu.clemson.resolve.analysis;

import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import edu.clemson.resolve.parser.ResolveParser;
import edu.clemson.resolve.parser.ResolveBaseListener;
import edu.clemson.resolve.proving.absyn.PExp;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Predicate;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.ModuleIdentifier;
import org.rsrg.semantics.ModuleScopeBuilder;
import org.rsrg.semantics.NoSuchModuleException;
import org.rsrg.semantics.programtype.PTType;
import org.rsrg.semantics.symbol.OperationSymbol;
import org.rsrg.semantics.symbol.Symbol;

import java.util.List;
import java.util.stream.Collectors;

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
        sanityCheckFunctionalOperationParameterModes(ctx.name, ctx.type(),
                ctx.operationParameterList().parameterDeclGroup());
        sanityCheckCalls(ctx, ctx.name);
    }

    /** Ensure the call we're looking at is not to another primary operation */
    public void sanityCheckCalls(@NotNull ParserRuleContext ctx,
                                 @NotNull Token name) {
        //get conceptual scope
        ModuleIdentifier parentConceptID = tr.getParentConceptIdentifier();
        try {
            ModuleScopeBuilder conceptualScope =
                    compiler.symbolTable.getModuleScope(parentConceptID);
            List<String> operationNames =
                    conceptualScope.getSymbolsOfType(OperationSymbol.class)
                            .stream().map(Symbol::getName)
                            .collect(Collectors.toList());

            CallCheckingListener searcher =
                    new CallCheckingListener(
                            e -> e.progNamedExp().qualifier == null &&
                                 operationNames.contains(e.progNamedExp().name.getText()) &&
                                 !e.progNamedExp().name.getText().equals(name.getText()));
            ParseTreeWalker.DEFAULT.walk(searcher, ctx);

            //if we found a call to a primary operation from another procedure
            if (searcher.result && searcher.foundCall != null) {
                compiler.errMgr.semanticError(
                        ErrorKind.ILLEGAL_PRIMARY_OPERATION_CALL,
                        searcher.foundCall.getStart(),
                        name.getText(),
                        searcher.foundCall.getText());
            }
        } catch (NoSuchModuleException e) {
            //that's fine, we just won't bother
        }
    }

    @Override public void exitOperationProcedureDecl(
            ResolveParser.OperationProcedureDeclContext ctx) {
        sanityCheckBlockEnds(ctx.name, ctx.closename);
        sanityCheckRecursiveProcKeyword(ctx, ctx.name, ctx.recursive);
        sanityCheckFunctionalOperationParameterModes(ctx.name, ctx.type(),
                ctx.operationParameterList().parameterDeclGroup());
    }

    @Override public void exitOperationDecl(
            ResolveParser.OperationDeclContext ctx) {
        sanityCheckFunctionalOperationParameterModes(ctx.name, ctx.type(),
                ctx.operationParameterList().parameterDeclGroup());
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
        boolean hasRecRef = hasRecursiveReferenceInStmts(ctx, name.getText());
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

    /** Checks to ensure that only the correct modes are used in the
     *  declaration of an operation {@code name} that has some return
     *  {@code type}.
     *  <p>
     *  Note: as far as I'm aware only {@code restores}, {@code preserves},
     *  and {@code evaluates} mode params are acceptable in this case;
     *  add with others as needed.</p>
     *
     * @param name the operation's name
     * @param type the declared return type
     * @param parameters the formal parameters
     */
    private void sanityCheckFunctionalOperationParameterModes(
            @NotNull Token name, @Nullable ResolveParser.TypeContext type,
            @NotNull List<ResolveParser.ParameterDeclGroupContext> parameters) {
        if (type == null) return;
        for (ResolveParser.ParameterDeclGroupContext group : parameters) {
            if (!(group.parameterMode().getText().equals("restores") ||
                  group.parameterMode().getText().equals("preserves") ||
                  group.parameterMode().getText().equals("evaluates"))) {
                List<String> paramNames = group.ID().stream()
                        .map(ParseTree::getText).collect(Collectors.toList());
                compiler.errMgr.semanticError(
                        ErrorKind.ILLEGAL_MODE_FOR_FUNCTIONAL_OP,
                        group.getStart(), name.getText(), paramNames,
                        group.parameterMode().getText());
            }
        }
    }

    /** Returns {@code true} if {@code name} appears in any programming call
     *  expression ({@link ResolveParser.ProgParamExpContext}) within parse
     *  context {@code ctx}.
     *  <p>
     *  One caveat: if we find a matching expression but it's
     *  <em>qualified</em>, we return {@code false} as this indicates we're
     *  not looking at a recursive call (even though it's named the same).</p>
     *
     *  @param ctx the search context
     *  @param name the name of the (potentially) recursive reference
     *  @return whether or not {@code name} is referenced
     *  recursively in {@code ctx}
     */
    private boolean hasRecursiveReferenceInStmts(@NotNull ParserRuleContext ctx,
                                                 @NotNull String name) {
        CallCheckingListener searcher =
                new CallCheckingListener(
                        e -> e.progNamedExp().qualifier == null &&
                             e.progNamedExp().name.getText().equals(name));
        ParseTreeWalker.DEFAULT.walk(searcher, ctx);
        return searcher.result;
    }

    protected static class CallCheckingListener
            extends
                ResolveBaseListener {
        private final Predicate<ResolveParser.ProgParamExpContext> checker;
        public boolean result = false;
        public ResolveParser.ProgParamExpContext foundCall = null;

        public CallCheckingListener(
                @NotNull Predicate<ResolveParser.ProgParamExpContext> checker) {
            this.checker = checker;
        }
        @Override public void exitProgParamExp(
                ResolveParser.ProgParamExpContext ctx) {
            if (checker.test(ctx)) {
                result = true;
                foundCall = ctx;
            }
        }
    }
}
