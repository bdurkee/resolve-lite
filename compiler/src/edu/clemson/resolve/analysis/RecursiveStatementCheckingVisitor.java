package edu.clemson.resolve.analysis;

import edu.clemson.resolve.parser.ResolveBaseVisitor;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * Visits statements within a parse tree context and looks for recursive
 * calls. The visit chain returns an {@code true} if a recursive call was found;
 * {@code false} otherwise.
 */
public class RecursiveStatementCheckingVisitor
        extends
            ResolveBaseVisitor<Boolean> {

   private final Token callName;

    public RecursiveStatementCheckingVisitor(Token callName) {
        this.callName = callName;
    }

    /* @Override public Boolean visitTypeImplInit(
            Resolve.TypeImplInitContext ctx) {
        return ctx.stmt().stream().anyMatch(this::visit);
    }

    @Override public Boolean visitOperationProcedureDecl(
            Resolve.OperationProcedureDeclContext ctx) {
        return ctx.stmt().stream().anyMatch(this::visit);
    }

    @Override public Boolean visitProcedureDecl(
            Resolve.ProcedureDeclContext ctx) {
        return ctx.stmt().stream().anyMatch(this::visit);
    }

    @Override public Boolean visitIfStmt(Resolve.IfStmtContext ctx) {
        boolean result = false;
        result = visit(ctx.progExp());
        if (!result) result = ctx.stmt().stream().anyMatch(this::visit);
        if (!result && ctx.elsePart() != null) {
            result = ctx.elsePart().stmt().stream().anyMatch(this::visit);
        }
        return result;
    }
    @Override public Boolean visitWhileStmt(Resolve.WhileStmtContext ctx) {
        boolean result = false;
        result = visit(ctx.progExp());
        if (!result) result = ctx.stmt().stream().anyMatch(this::visit);
        return result;
    }

    @Override public Boolean visitStmt(Resolve.StmtContext ctx) {
        return visit(ctx.getChild(0));
    }

    @Override public Boolean visitAssignStmt(Resolve.AssignStmtContext ctx) {
        return visit(ctx.progExp()); //visit rhs only (left hand side is just vars)
    }

    @Override public Boolean visitCallStmt(Resolve.CallStmtContext ctx) {
        return visit(ctx.progExp());
    }

    @Override public Boolean visitProgPrimary(Resolve.ProgPrimaryContext ctx) {
        return visit(ctx.getChild(0));
    }

    @Override public Boolean visitProgPrimaryExp(
            Resolve.ProgPrimaryExpContext ctx) {
        return visit(ctx.progPrimary());
    }

    @Override public Boolean visitProgParamExp(
            Resolve.ProgParamExpContext paramExp) {
        return paramExp.name.getText().equals(callName.getText()) &&
                (paramExp.qualifier == null);
        //recursive calls are must be to a local operation (disregarding mutual rec.)
    }

    @Override public Boolean visitTerminal(TerminalNode t) {
        return false;
    }*/
}
