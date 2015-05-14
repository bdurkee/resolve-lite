package org.resolvelite.vcgen.applicationstrategies;

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
import org.resolvelite.vcgen.model.AssertiveCode;
import org.resolvelite.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;

import java.util.Collections;
import java.util.List;
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

        if (rightReplacer.isLiteral()) {
            PExp workingConfirm = block.finalConfirm.getContents();
            block.finalConfirm(workingConfirm
                    .substitute(leftReplacee, rightReplacer));
            return block.snapshot();
        }
        //At this point the rhs of the assign must be a function call.
        PSymbol asPSym = (PSymbol) rightReplacer;
        OperationSymbol op = getOperation(block.scope, asPSym);

        List<PExp> actuals = asPSym.getArguments();
        List<PExp> formals = op.getParameters().stream()
                .map(ProgParameterSymbol::asPSymbol).collect(Collectors.toList());

        /**
         * So: {@pre Oper op (x: T): U; pre /_x_\; post op = f/_x_\} is in Ctx
         * and our statement reads as follows: {@code v := op(u);}. Informally
         * this next line substitutes appearances of the formal parameter
         * {@code x} appearing in op's requires clause with the actuals
         * (more formally, {@code pre[x -> u]}).
         */
        PExp opRequires = annotations.getPExpFor(block.g, op.getRequires())
                .substitute(formals, actuals);
        block.confirm(opRequires);

        PExp opEnsures = annotations.getPExpFor(block.g, op.getEnsures())
                .substitute(formals, actuals);

        //update our list of formal vars to account for incoming vars in the
        //ensures clause
        for (PSymbol f : opEnsures.getIncomingVariables()) {
            Collections.replaceAll(formals, f.withIncomingSignsErased(), f);
        }
        /**
         * Now we substitute the formals for actuals in the ensures clause
         * {@code f}, and replace all occurences of {@code v} in {@code Q}
         * with the modified {@code f} (formally, {@code Q[v -> f[x -> u]]}).
         */


        return block.snapshot();
    }

    private OperationSymbol getOperation(Scope s, PSymbol rhs) {
        List<PTType> argTypes = rhs.getArguments().stream()
                .map(PExp::getProgType).collect(Collectors.toList());
        try {
            //Todo: Pass in the qualifier once I add it to PSymbol
            return s.queryForOne(new OperationQuery(null,
                    rhs.getName(), argTypes));
        }
        catch (NoSuchSymbolException|DuplicateSymbolException e) {
            throw new RuntimeException(e);
        }
    }

    @Override public String getDescription() {
        return "function assignment rule application";
    }
}
