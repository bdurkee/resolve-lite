package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.vcgen.ModelBuilderProto;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCRuleBackedStat;
import org.rsrg.semantics.symbol.OperationSymbol;
import org.rsrg.semantics.symbol.ProgParameterSymbol;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FunctionAssignApplicationStrategy
        implements
            StatRuleApplicationStrategy<VCRuleBackedStat> {

    @Override public AssertiveBlock applyRule(
            VCAssertiveBlock.VCAssertiveBlockBuilder block,
            VCRuleBackedStat stat) {
        PExp leftReplacee = stat.getStatComponents().get(0);
        PExp rightReplacer = stat.getStatComponents().get(1);

        if ( !(rightReplacer.isFunctionApplication()) ) {
            PExp workingConfirm = block.finalConfirm.getConfirmExp();
            block.finalConfirm(workingConfirm.substitute(leftReplacee,
                    rightReplacer));
            return block.snapshot();
        }
        PSymbol call = (PSymbol)rightReplacer;
        //we know rightReplacer is a function app, see if-catch above.
        OperationSymbol op = ExplicitCallApplicationStrategy
                .getOperation(block.scope, (PSymbol) rightReplacer);

        List<PExp> actuals = call.getArguments();
        List<PExp> formals = op.getParameters().stream()
                .map(ProgParameterSymbol::asPSymbol).collect(Collectors.toList());
        /**
         * So: {@pre Oper op (x: T): U; pre /_x_\; post op = f/_x_\} is in Ctx
         * and our statement reads as follows: {@code v := op(u);}. Informally
         * this next line substitutes appearances of the formal parameter
         * {@code x} in op's requires clause with the actuals (more formally,
         * {@code pre[x ~> u]}).
         */
        PExp opRequires = block.getPExpFor(op.getRequires()).substitute(
                ModelBuilderProto.getFacilitySpecializations(
                        block.symtab.mathPExps,
                        block.scope, call.getQualifier()));
        opRequires = opRequires.substitute(formals, actuals);
        block.confirm(opRequires.substitute(formals, actuals));

        PExp opEnsures = block.getPExpFor(op.getEnsures());

        if (opEnsures.isObviouslyTrue()) return block.snapshot();

        //TODO: We had better check the form of the ensures clauses on ops
        //that return something. Should just be an equality: <opname> = <expr>;
        PExp ensuresRight = opEnsures.getSubExpressions().get(1);

        //update our list of formal params to account for incoming-valued refs
        //to themselves in the ensures clause
        for (PSymbol f : ensuresRight.getIncomingSymbols()) {
            Collections.replaceAll(formals, f.withIncomingSignsErased(), f);
        }

        /**
         * Now we substitute the formals for actuals in the rhs of the ensures
         * ({@code f}), THEN replace all occurences of {@code v} in {@code Q}
         * with the modified {@code f} (formally, {@code Q[v ~> f[x ~> u]]}).
         */
        ensuresRight = ensuresRight.substitute(formals, actuals);

        block.finalConfirm(block.finalConfirm.getConfirmExp()
                .substitute(leftReplacee, ensuresRight));
        return block.snapshot();
    }

    @Override public String getDescription() {
        return "function assignment rule application";
    }
}