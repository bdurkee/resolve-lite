package org.resolvelite.vcgen.applicationstrategies;

import org.antlr.v4.runtime.ParserRuleContext;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.proving.absyn.PSymbol;
import org.resolvelite.semantics.DuplicateSymbolException;
import org.resolvelite.semantics.NoSuchSymbolException;
import org.resolvelite.semantics.Scope;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.semantics.query.OperationQuery;
import org.resolvelite.semantics.symbol.OperationSymbol;
import org.resolvelite.semantics.symbol.ProgParameterSymbol;
import org.resolvelite.vcgen.ModelBuilder;
import org.resolvelite.vcgen.model.AssertiveCode;
import org.resolvelite.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FunctionAssignApplicationStrategy
        implements
            RuleApplicationStrategy<ResolveParser.AssignStmtContext> {

    @Override public AssertiveCode applyRule(
            ResolveParser.AssignStmtContext statement,
            VCAssertiveBlockBuilder block) {

        AnnotatedTree annotations = block.annotations;
        PExp leftReplacee = annotations.mathPExps.get(statement.left);
        PExp rightReplacer = annotations.mathPExps.get(statement.right);

        if ( rightReplacer.isLiteral() ) {
            PExp workingConfirm = block.finalConfirm.getContents();
            block.finalConfirm(workingConfirm.substitute(leftReplacee,
                    rightReplacer));
            return block.snapshot();
        }

        //apply explicit call rule to the 'exp-call-like-thing' on the rhs.
        return new ExplicitCallApplicationStrategy<ParserRuleContext>()
                .applyRule(statement.right, block);
    }

    @Override public String getDescription() {
        return "function assignment rule application";
    }
}
