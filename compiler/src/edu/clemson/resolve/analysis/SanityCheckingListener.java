package edu.clemson.resolve.analysis;

import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import edu.clemson.resolve.parser.Resolve;
import edu.clemson.resolve.parser.ResolveBaseListener;
import edu.clemson.resolve.parser.ResolveBaseVisitor;
import edu.clemson.resolve.proving.absyn.PExp;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.rsrg.semantics.programtype.PTType;

/**
 * Uses a combination of listeners and visitors to check for some semantic
 * errors uncheckable in the grammar and ommited by {@link PopulatingVisitor}.
 * <p>
 * To make many of our checks here easier, we make sure to have already built
 * the ast for exprs in a previous phase.</p>
 */
public class SanityCheckingListener extends ResolveBaseListener {

    private final RESOLVECompiler compiler;
    private final AnnotatedTree tr;

    public SanityCheckingListener(RESOLVECompiler rc, AnnotatedTree tr) {
        this.compiler = rc;
        this.tr = tr;
    }

    @Override public void exitConceptModule(Resolve.ConceptModuleContext ctx) {
        sanityCheckBlockEnds(ctx.name, ctx.closename);
    }

    @Override public void exitEnhancementModule(
            Resolve.EnhancementModuleContext ctx) {
        sanityCheckBlockEnds(ctx.name, ctx.closename);
    }

    @Override public void exitFacilityModule(
            Resolve.FacilityModuleContext ctx) {
        sanityCheckBlockEnds(ctx.name, ctx.closename);
    }

    @Override public void exitConceptImplModule(
            Resolve.ConceptImplModuleContext ctx) {
        sanityCheckBlockEnds(ctx.name, ctx.closename);
    }

    @Override public void exitEnhancementImplModule(
            Resolve.EnhancementImplModuleContext ctx) {
        sanityCheckBlockEnds(ctx.name, ctx.closename);
    }

    @Override public void exitPrecisModule(Resolve.PrecisModuleContext ctx) {
        sanityCheckBlockEnds(ctx.name, ctx.closename);
    }

    @Override public void exitProcedureDecl(Resolve.ProcedureDeclContext ctx) {
        sanityCheckBlockEnds(ctx.name, ctx.closename);
        sanityCheckRecursiveProcKeyword(ctx, ctx.name, ctx.recursive);
    }

    @Override public void exitOperationProcedureDecl(
            Resolve.OperationProcedureDeclContext ctx) {
        sanityCheckBlockEnds(ctx.name, ctx.closename);
        sanityCheckRecursiveProcKeyword(ctx, ctx.name, ctx.recursive);
    }

    @Override public void exitAssignStmt(Resolve.AssignStmtContext ctx) {
        sanityCheckProgOpTypes(ctx, tr.progTypes.get(ctx.left),
                tr.progTypes.get(ctx.right));
    }

    @Override public void exitSwapStmt(Resolve.SwapStmtContext ctx) {
        sanityCheckProgOpTypes(ctx,tr.progTypes.get(ctx.left),
                tr.progTypes.get(ctx.right));
    }

    @Override public void exitRequiresClause(Resolve.RequiresClauseContext ctx) {
        PExp requires = tr.mathPExps.get(ctx);
        if (requires != null && !requires.getIncomingVariables().isEmpty()) {
            compiler.errMgr.semanticError(
                    ErrorKind.ILLEGAL_INCOMING_REF_IN_REQUIRES, ctx.getStart(),
                    requires.getIncomingVariables(),
                    ctx.mathAssertionExp().getText());
        }
    }

    private void sanityCheckProgOpTypes(ParserRuleContext ctx,
                                        PTType l, PTType r) {
        if (!l.equals(r)) {
            compiler.errMgr.semanticError(ErrorKind.INCOMPATIBLE_OP_TYPES,
                    ctx.getStart(), ctx.getText(), l.toString(), r.toString());
        }
    }

    private void sanityCheckRecursiveProcKeyword(ParserRuleContext ctx,
                                                 Token name,
                                                 Token recursiveToken) {
        boolean hasRecRef = hasRecursiveReference(ctx, name);
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

    private void sanityCheckBlockEnds(Token topName, Token bottomName) {
        if (!topName.getText().equals(bottomName.getText())) {
            compiler.errMgr.semanticError(
                    ErrorKind.MISMATCHED_BLOCK_END_NAMES, bottomName,
                    topName.getText(), bottomName.getText());
        }
    }

    private boolean hasRecursiveReference(ParserRuleContext ctx, Token name) {
        return new ResolveBaseVisitor<Boolean>() {
            @Override public Boolean visitOperationProcedureDecl(
                    Resolve.OperationProcedureDeclContext ctx) {
                return ctx.stmt().stream().anyMatch(this::visit);
            }
            @Override public Boolean visitProcedureDecl(
                    Resolve.ProcedureDeclContext ctx) {
                return ctx.stmt().stream().anyMatch(this::visit);
            }
            @Override public Boolean visitStmt(Resolve.StmtContext ctx) {
                return visit(ctx.getChild(0));
            }
            @Override public Boolean visitCallStmt(Resolve.CallStmtContext ctx) {
                return visit(ctx.progExp());
            }
            @Override public Boolean visitProgPrimary(Resolve.ProgPrimaryContext ctx) {
                return visit(ctx.getChild(0));
            }
            @Override public Boolean visitProgPrimaryExp(Resolve.ProgPrimaryExpContext ctx) {
                return visit(ctx.progPrimary());
            }
            @Override public Boolean visitProgParamExp(
                    Resolve.ProgParamExpContext paramExp) {
                return paramExp.name.getText().equals(name.getText()) &&
                        (paramExp.qualifier == null);
                //recursive calls are must be to a local operation (disregarding mutual rec.)
            }
            @Override public Boolean visitTerminal(TerminalNode var1) {
                return false;
            }
        }.visit(ctx);
    }

}
