package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.parser.ResolveLexer;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCRuleBackedStat;
import org.antlr.v4.runtime.CommonToken;
import org.rsrg.semantics.DuplicateSymbolException;
import org.rsrg.semantics.NoSuchSymbolException;
import org.rsrg.semantics.Scope;
import org.rsrg.semantics.programtype.PTType;
import org.rsrg.semantics.query.OperationQuery;
import org.rsrg.semantics.symbol.OperationSymbol;
import org.rsrg.semantics.symbol.ProgParameterSymbol;

import java.util.*;
import java.util.stream.Collectors;

public class ExplicitCallApplicationStrategy
        implements
            StatRuleApplicationStrategy<VCRuleBackedStat> {

    @Override public AssertiveBlock applyRule(
            VCAssertiveBlockBuilder block, VCRuleBackedStat stat) {
        PSymbol callExp = (PSymbol) stat.getStatComponents().get(0);
        OperationSymbol op = getOperation(block.scope, callExp);

        PExp modifiedEnsures = modifyExplicitCallEnsures(block, callExp);
        if (modifiedEnsures.equals(block.g.getTrueExp())) return block.snapshot();

        Map<String, ProgParameterSymbol.ParameterMode> modes =
                new LinkedHashMap<>();

        //The collection of 'updates' actuals that should be replaced in the
        //existing final confirm.
        Iterator<ProgParameterSymbol> formalParamIter =
                op.getParameters().iterator();
        Iterator<PExp> actualParamIter = callExp.getArguments().iterator();
        List<PExp> replacementActuals = new ArrayList<>();

        while (formalParamIter.hasNext()) {
            ProgParameterSymbol formal = formalParamIter.next();
            PExp actual = actualParamIter.next();
            if (formal.getMode() == ProgParameterSymbol.ParameterMode.UPDATES) {
                replacementActuals.add(actual);
            }
        }
        block.finalConfirm(block.finalConfirm.getConfirmExp()
                .substitute(replacementActuals, modifiedEnsures));
        return block.snapshot();
    }

    /**
     * In the explicit call rule, this helper method (used by both the
     * explicit call rule & the function assignment rule) simply returns the
     * result of the {@code f[x ~> u]} part in the overall
     * step: {@code Q[v ~> f[x ~> u]]}.
     */
    protected static PExp modifyExplicitCallEnsures(VCAssertiveBlockBuilder block,
                                                    PSymbol call) {
        AnnotatedTree annotations = block.annotations;
        OperationSymbol op = getOperation(block.scope, call);

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
        PExp opRequires = annotations.getPExpFor(block.g, op.getRequires());
        block.confirm(opRequires.substitute(formals, actuals));

        PSymbol opEnsures = (PSymbol)annotations
                .getPExpFor(block.g, op.getEnsures());
        if (opEnsures.isObviouslyTrue()) return opEnsures;

        List<PExp> con = block.finalConfirm.getConfirmExp().splitIntoConjuncts();

        PExp ensuresLeft = opEnsures.getArguments().get(0);
        PExp ensuresRight = opEnsures.getArguments().get(1);

        //update our list of formal params to account for incoming-valued refs
        //to themselves in the ensures clause
        for (PSymbol f : ensuresRight.getIncomingVariables()) {
            Collections.replaceAll(formals, f.withIncomingSignsErased(), f);
        }

        /**
         * Now we substitute the formals for actuals in the rhs of the ensures
         * ({@code f}), THEN replace all occurences of {@code v} in {@code Q}
         * with the modified {@code f} (formally, {@code Q[v ~> f[x ~> u]]}).
         */
        ensuresRight = ensuresRight.substitute(formals, actuals);
        return ensuresRight;
    }

    protected static OperationSymbol getOperation(Scope s, PSymbol app) {
        List<PTType> argTypes = app.getArguments().stream()
                .map(PExp::getProgType).collect(Collectors.toList());
        try {
            return s.queryForOne(new OperationQuery(
                    (app.getQualifier() != null) ?
                            new CommonToken(ResolveLexer.ID, app.getQualifier()) : null,
                        app.getName(), argTypes));
        }
        catch (NoSuchSymbolException|DuplicateSymbolException e) {
            //shouldn't happen. Well, depends on s.
            throw new RuntimeException(e);
        }
    }

    @Override public String getDescription() {
        return "explicit (simple) call rule application";
    }
}
