package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.model.VCRuleBackedStat;
import org.jetbrains.annotations.NotNull;
import edu.clemson.resolve.semantics.symbol.OperationSymbol;
import edu.clemson.resolve.semantics.symbol.ProgParameterSymbol;

import java.util.Collections;
import java.util.List;

public class FunctionAssignApplicationStrategy implements StatRuleApplicationStrategy<VCRuleBackedStat> {

    //TODO: Walk through this step by step in a .md file. Then store the .md file in docs/
    @NotNull
    @Override
    public AssertiveBlock applyRule(@NotNull VCAssertiveBlockBuilder block, @NotNull VCRuleBackedStat stat) {
        PExp leftReplacee = stat.getStatComponents().get(0);
        PExp rightReplacer = stat.getStatComponents().get(1);

        if (!(rightReplacer.isFunctionApplication())) {
            PExp workingConfirm = block.finalConfirm.getConfirmExp();
            block.finalConfirm(workingConfirm.substitute(leftReplacee,
                    rightReplacer));
            return block.snapshot();
        }
        PApply call = (PApply) rightReplacer;
        //we know rightReplacer is a function app, see if-catch above.
        OperationSymbol op = ExplicitCallApplicationStrategy
                .getOperation(block.scope, (PApply) rightReplacer);

        List<PExp> actuals = call.getArguments();
        List<PExp> formals = Utils.apply(op.getParameters(), ProgParameterSymbol::asPSymbol);

        /** So: {@pre Oper op (x: T): U; pre /_x_\; post op = f/_x_\} is in Ctx
         *  and our statement reads as follows: {@code v := op(u);}. Informally
         *  this next line substitutes appearances of the formal parameter
         *  {@code x} in op's requires clause with the actuals (more formally,
         *  {@code pre[x ~> u]}).
         */
        PExp opRequires = op.getRequires().substitute(
                block.getSpecializationsForFacility(
                        ((PSymbol) call.getFunctionPortion()).getQualifier()));
        block.confirm(opRequires.substitute(formals, actuals));

        PExp opEnsures = op.getEnsures();
        if (opEnsures.isObviouslyTrue()) return block.snapshot();

        //TODO: We had better check the form of the ensures clauses on ops
        //that return something. Should just be an equality: <opname> = <expr>;
        PExp ensuresRight = opEnsures.getSubExpressions().get(2);

        //update our list of formal params to account for incoming-valued refs
        //to themselves in the ensures clause
        for (PSymbol f : ensuresRight.getIncomingVariables()) {
            Collections.replaceAll(formals, f.withIncomingSignsErased(), f);
        }

        /** Now we substitute the formals for actuals in the rhs of the ensures
         *  ({@code f}), THEN replace all occurences of {@code v} in {@code Q}
         *  with the modified {@code f} (formally, {@code Q[v ~> f[x ~> u]]}).
         */
        ensuresRight = ensuresRight.substitute(formals, actuals);

        block.finalConfirm(block.finalConfirm.getConfirmExp()
                .substitute(leftReplacee, ensuresRight));
        return block.snapshot();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "function assignment rule application";
    }
}