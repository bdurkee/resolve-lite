package org.resolvelite.vcgen.applicationstrategies;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.pattern.ParseTreePattern;
import org.antlr.v4.runtime.tree.xpath.XPath;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.misc.Utils;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.semantics.DuplicateSymbolException;
import org.resolvelite.semantics.NoSuchSymbolException;
import org.resolvelite.semantics.Scope;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.semantics.query.OperationQuery;
import org.resolvelite.semantics.symbol.OperationSymbol;
import org.resolvelite.semantics.symbol.ProgParameterSymbol;
import org.resolvelite.vcgen.model.AssertiveCode;
import org.resolvelite.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class FunctionAssignApplicationStrategy
        implements
            RuleApplicationStrategy<ResolveParser.AssignStmtContext> {

    //Todo: Look into using xpath here. I think it'd really clean up the logic.
    @Override public AssertiveCode applyRule(
            ResolveParser.AssignStmtContext statement,
            VCAssertiveBlockBuilder block) {
        AnnotatedTree annotations = block.annotations;
        PExp leftReplacee =
                block.annotations.mathPExps.get(statement.left);
        PExp rightReplacer =
                block.annotations.mathPExps.get(statement.right);
        ParseTree topLevelChild = statement.getChild(0);

        if (isRightSideLiteral(statement)) {
            PExp workingConfirm = block.finalConfirm.getContents();
            block.finalConfirm(workingConfirm
                    .substitute(leftReplacee, rightReplacer));
            return block.snapshot();
        }
        OperationSymbol formalOp = getOperation(block, statement);
        PExp requires = block.annotations.getPExpFor(block.g,
                formalOp.getRequires());

        List<PExp> actuals = getArgs(statement).stream()
                .map(block.annotations.mathPExps::get).collect(Collectors.toList());
        List<PExp> formals = formalOp.getParameters().stream()
                .map(ProgParameterSymbol::asPExp).collect(Collectors.toList());

        return block.snapshot();
    }

    private List<ResolveParser.ProgExpContext> getArgs(
            ResolveParser.AssignStmtContext ctx) {
        List<ResolveParser.ProgExpContext> args = new ArrayList<>();
        ParseTree r = ctx.right.getPayload();
        if ( r instanceof ResolveParser.ProgPrimaryExpContext
                && !isRightSideLiteral(ctx) ) {
            ResolveParser.ProgParamExpContext p =
                    ((ResolveParser.ProgParamExpContext) r.getChild(0)
                            .getChild(0));
            args = p.progExp();
        }
        else {
            ResolveParser.ProgApplicationExpContext p =
                    ((ResolveParser.ProgApplicationExpContext) r.getChild(0));
            args = p.progExp();
        }
        return args;
    }

    private OperationSymbol getOperation(VCAssertiveBlockBuilder block,
                                         ResolveParser.AssignStmtContext ctx) {
        List<ResolveParser.ProgExpContext> args = new ArrayList<>();
        Token qualifier = null;
        String name = null;
        ParseTree r = ctx.right.getPayload();
        if (r instanceof ResolveParser.ProgPrimaryExpContext &&
                !isRightSideLiteral(ctx)) {
            ResolveParser.ProgParamExpContext p =
                    ((ResolveParser.ProgParamExpContext)r.getChild(0).getChild(0));
            qualifier = p.qualifier;
            name = p.name.getText();
            args = p.progExp();
        }
        else {
            ResolveParser.ProgApplicationExpContext p =
                    ((ResolveParser.ProgApplicationExpContext)r.getChild(0));
            args = p.progExp();
            name = Utils.getNameFromProgramOp(p.op.getText());
        }
        List<PTType> argTypes = args.stream()
                .map(block.annotations.progTypes::get)
                .collect(Collectors.toList());
        try {
            return block.scope.queryForOne(
                    new OperationQuery(qualifier, name, argTypes));
        } catch (NoSuchSymbolException|DuplicateSymbolException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isRightSideLiteral(ResolveParser.AssignStmtContext ctx) {
        ParseTree r = ctx.right.getPayload();
        return r instanceof ResolveParser.ProgPrimaryExpContext
                && !(r.getChild(0).getChild(0) instanceof ResolveParser.ProgParamExpContext);
    }

    @Override public String getDescription() {
        return "function assignment rule application";
    }
}
