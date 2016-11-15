package edu.clemson.resolve.vcgen.app;

import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpListener;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.semantics.symbol.OperationSymbol;
import edu.clemson.resolve.semantics.symbol.ProgParameterSymbol;
import edu.clemson.resolve.vcgen.VCAssertiveBlock;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.stats.VCAssign;
import edu.clemson.resolve.vcgen.stats.VCConfirm;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class FunctionAssignApplicationStrategy implements RuleApplicationStrategy<VCAssign> {

    //TODO: Walk through this step by step in a .md file. Then store the .md file in docs/
    @NotNull
    @Override
    public VCAssertiveBlock applyRule(@NotNull Deque<VCAssertiveBlockBuilder> accumulator,
                                      @NotNull VCAssertiveBlockBuilder block,
                                      @NotNull VCAssign stat) {
        PExp left = stat.getLeft();
        PExp right = stat.getRight();

        if (!(right.isFunctionApplication())) {
            VCConfirm workingConfirm = block.finalConfirm.withSequentFormulaSubstitution(left, right);
            return block.finalConfirm(workingConfirm).snapshot();
        }

        //Apply the listener to the (potentially) nested calls on the right hand side.
        Invk_Cond l = new Invk_Cond(stat.getDefiningContext(), block);
        right.accept(l);

        //Q[v ~> f[x ~> u]]).
        block.finalConfirm(block.finalConfirm.withSequentFormulaSubstitution(left, l.substitutions.get(right)));
        return block.snapshot();
    }

    //TODO for the explicit and general call rule applications, we'll need to iterate over the params, and
    //whenever we see an evaluates mode parameter, this is where we'll invoke this rule to applyBackingRule that..
    public static class Invk_Cond extends PExpListener {
        private final ParserRuleContext ctx;
        private final VCAssertiveBlockBuilder block;
        public final Map<PExp, PExp> substitutions = new HashMap<>();

        public Invk_Cond(ParserRuleContext ctx, VCAssertiveBlockBuilder block) {
            this.block = block;
            this.ctx = ctx;
        }

        @Override
        public void endPApply(@NotNull PApply e) {
            OperationSymbol op = GeneralCallApplicationStrategy.getOperation(block.scope, (PApply) e);

            PApply eSubstituted = (PApply) e.substitute(substitutions);

            List<PExp> actuals = eSubstituted.getArguments();
            List<PExp> formals = Utils.apply(op.getParameters(), ProgParameterSymbol::asPSymbol);

            //So: Oper op (x: T): U; pre /_x_\; post op = f/_x_\ is in Ctx and our statement reads as follows:
            //v := op(u);. Informally this next line substitutes appearances of the formal parameter x in op's
            //requires clause with the actuals (more formally, pre[x ~> u]).
            PExp opRequires = op.getRequires().substitute(
                    block.getSpecializationsForFacility(((PSymbol)e.getFunctionPortion()).getQualifier()));
            if (!opRequires.isObviouslyTrue()) {
                block.confirm(ctx, opRequires.substitute(formals, actuals)
                        .withVCInfo(ctx.getStart(), "Requires clause for call " + e));
            }

            PExp opEnsures = op.getEnsures();
            if (opEnsures.isObviouslyTrue()) substitutions.put(e, block.g.getTrueExp());

            //TODO: We had better check the form of the ensures clauses on ops
            //that return something. Should just be an equality: <opname> = <expr>;
            PExp ensuresRight = opEnsures.getSubExpressions().get(2);

            //update our list of formal params to account for incoming-valued refs
            //to themselves in the ensures clause
            for (PSymbol f : ensuresRight.getIncomingVariables()) {
                Collections.replaceAll(formals, f.withIncomingSignsErased(), f);
            }
            //v := op(u);   then the op = Oper op (x); ensures op = f/_x_\;
            //Now we substitute the formals for actuals in the rhs of the ensures (f),
            ensuresRight = ensuresRight.substitute(formals, actuals);
            substitutions.put(e, ensuresRight);
        }

        public PExp mathFor(PExp programmingExp) {
            return substitutions.get(programmingExp);
        }
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Function assignment rule app";
    }
}