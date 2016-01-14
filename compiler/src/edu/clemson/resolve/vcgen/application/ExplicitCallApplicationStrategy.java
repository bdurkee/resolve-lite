package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.ResolveLexer;
import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpListener;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCRuleBackedStat;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.Scope;
import org.rsrg.semantics.SymbolTableException;
import org.rsrg.semantics.query.OperationQuery;
import org.rsrg.semantics.symbol.OperationSymbol;
import org.rsrg.semantics.symbol.ProgParameterSymbol;

import java.util.*;

/** An explicit call application is for calls to ops with
 *  1. no return
 *  2. whose ensure's clause consists of only equality exprs whose lhs is a
 *     variable from a parameter having mode updates
 *  See {@link edu.clemson.resolve.vcgen.ModelBuilderProto#inSimpleForm(PExp, List)} for
 *  more info on what consitutes a call as 'simple' or explicit.
 */
public class ExplicitCallApplicationStrategy
        implements
            StatRuleApplicationStrategy<VCRuleBackedStat> {

    @NotNull @Override public AssertiveBlock applyRule(
            @NotNull VCAssertiveBlockBuilder block,
            @NotNull VCRuleBackedStat stat) {
        PApply callExp = (PApply) stat.getStatComponents().get(0);
        ExplicitCallRuleApplyingListener applier =
                new ExplicitCallRuleApplyingListener(block);
        callExp.accept(applier);

        return block.finalConfirm(applier.getCompletedExp())
                .snapshot();
    }

    @NotNull @Override public String getDescription() {
        return "explicit call rule application";
    }

    public static OperationSymbol getOperation(Scope s, PApply app) {
        PSymbol name = (PSymbol)app.getFunctionPortion();
        Token qualifier = (name.getQualifier() != null) ?
                new CommonToken(ResolveLexer.ID, name.getQualifier()) : null;
        try {
            return s.queryForOne(new OperationQuery(qualifier, name.getName(),
                    Utils.apply(app.getArguments(), PExp::getProgType)));
        }
        catch (SymbolTableException e) {
            //shouldn't happen; well, depends on s.
            throw new RuntimeException(e);
        }
    }

    //TODO: Walk through this step by step in a .md file. Then store the .md file in docs/
    public static class ExplicitCallRuleApplyingListener extends PExpListener {
        public Map<PExp, PExp> returnEnsuresArgSubstitutions = new HashMap<>();
        private final VCAssertiveBlock.VCAssertiveBlockBuilder block;

        public ExplicitCallRuleApplyingListener(
                VCAssertiveBlock.VCAssertiveBlockBuilder block) {
            this.block = block;
        }

        public PExp getCompletedExp() {
            return block.finalConfirm.getConfirmExp();
        }

        @Override public void endPApply(@NotNull PApply e) {
            PSymbol name = (PSymbol) e.getFunctionPortion();
            returnEnsuresArgSubstitutions.clear(); //TODO: hmmmm..
            List<PExp> actuals = e.getArguments();

            PSymbol functionName = (PSymbol) e.getFunctionPortion();
            OperationSymbol op = getOperation(block.scope, e);

            List<PExp> formals = Utils.apply(op.getParameters(),
                    ProgParameterSymbol::asPSymbol);
            PExp opRequires = op.getRequires().substitute(formals, actuals);
            opRequires = opRequires.substitute(
                    block.getSpecializationsForFacility(name.getQualifier()));
            block.confirm(opRequires);

            PExp opEnsures = op.getEnsures();
            Iterator<ProgParameterSymbol> formalParamIter =
                    op.getParameters().iterator();
            Iterator<PExp> actualParamIter = e.getArguments().iterator();

            Map<PExp, PExp> intermediateBindings = new LinkedHashMap<>();
            Map<String, PExp> ensuresEqualities =
                    opEnsures.getTopLevelVariableEqualities();

            //TODO: I don't think this will actually happen here. What we (were) worried about here is
            //what the FunctionAssign application is responsible for.
            //I think this 'if' (and its body) below should be erased.
            if (ensuresEqualities.containsKey(functionName.getName())) {
                intermediateBindings.put(e,
                        ensuresEqualities.get(functionName.getName()));
            }

            while (formalParamIter.hasNext()) {
                ProgParameterSymbol formal = formalParamIter.next();
                PExp actual = actualParamIter.next();
                if (formal.getMode() == ProgParameterSymbol.ParameterMode.UPDATES) {
                    if (!ensuresEqualities.containsKey(formal.getName())) {
                        continue;
                    }
                    intermediateBindings.put(actual,
                            ensuresEqualities.get(formal.getName()));
                }
            }
            for (Map.Entry<PExp, PExp> exp : intermediateBindings.entrySet()) {
                //update our list of formal params to account for incoming-valued refs
                //to themselves in the ensures clause
                List<PExp> varsToReplaceInEnsures = new ArrayList<>(formals);
                for (PSymbol f : exp.getValue().getIncomingVariables()) {
                    Collections.replaceAll(varsToReplaceInEnsures,
                            f.withIncomingSignsErased(), f);
                }
                /**
                 * Now we substitute the formals for actuals in the rhs of the ensures
                 * ({@code f}), THEN replace all occurences of {@code v} in {@code Q}
                 * with the modified {@code f}s (formally, {@code Q[v ~> f[x ~> u]]}).
                 */
                PExp v = exp.getValue().substitute(
                        varsToReplaceInEnsures, actuals);
                returnEnsuresArgSubstitutions.put(exp.getKey(), v);
            }
            PExp existingConfirm = block.finalConfirm.getConfirmExp();
            block.finalConfirm(existingConfirm.substitute(returnEnsuresArgSubstitutions));
        }
    }
}
