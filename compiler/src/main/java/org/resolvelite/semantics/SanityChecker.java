package org.resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.resolvelite.compiler.ErrorKind;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.parsing.ResolveBaseListener;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.proving.absyn.PSymbol;
import org.resolvelite.semantics.programtype.PTInvalid;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.semantics.symbol.OperationSymbol;

//Todo: Sanity checker jason was gonna write.
//Todo: Note that there should be no type assignment in here. Only checking.
public class SanityChecker extends ResolveBaseListener {

    private final ResolveCompiler compiler;
    private final SymbolTable symtab;
    private final AnnotatedTree tr;

    public SanityChecker(ResolveCompiler rc, AnnotatedTree tr) {
        this.compiler = rc;
        this.symtab = rc.symbolTable;
        this.tr = tr;
    }

    @Override public void exitOperationDecl(
            @NotNull ResolveParser.OperationDeclContext ctx) {

    }

    @Override public void exitOperationProcedureDecl(
            @NotNull ResolveParser.OperationProcedureDeclContext ctx) {
        if (ctx.ensuresClause() != null) {

        }
    }

    @Override public void exitAssignStmt(
            @NotNull ResolveParser.AssignStmtContext ctx) {
        checkTypes(ctx, ctx.left, ctx.right);
    }

    @Override public void exitSwapStmt(
            @NotNull ResolveParser.SwapStmtContext ctx) {
        checkTypes(ctx, ctx.left, ctx.right);
    }

    protected void checkTypes(@NotNull ParserRuleContext parent,
                              @NotNull ResolveParser.ProgExpContext t1,
                              @NotNull ResolveParser.ProgExpContext t2) {
        PTType leftType = tr.progTypes.get(t1);
        PTType rightType = tr.progTypes.get(t2);
        if ( !leftType.acceptableFor(rightType) ) {
            compiler.errorManager.semanticError(ErrorKind.INCOMPATIBLE_TYPES,
                    parent.getStart(), t1.getText(), leftType, t2.getText(),
                    rightType, parent.getText());
        }
    }

    protected void sanityCheckEnsuresClause(OperationSymbol op,
                                          PExp ensures) {
        boolean result = ensures.isFunction() && op.getEnsures() != null;
        if (result) {
            PSymbol s = ((PSymbol)ensures);
            result = s.getName().equals("=")
                    && s.getArguments().get(0).toString().equals(op.getName());
        }

        if (!result) {
            ResolveParser.EnsuresClauseContext ens = op.getEnsures();
            compiler.errorManager.semanticError(ErrorKind.MALFORMED_ENSURES_CLAUSE,
                    ((ParserRuleContext) op.getDefiningTree()).getStart(),
                    op.getName(), ens == null ? "null" :
                            ens.mathAssertionExp().getText());
        }
    }

    //protected void sanityCheckEnsuresClause();
}
