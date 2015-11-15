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
import org.rsrg.semantics.DuplicateSymbolException;
import org.rsrg.semantics.NoSuchSymbolException;
import org.rsrg.semantics.Scope;
import org.rsrg.semantics.query.OperationQuery;
import org.rsrg.semantics.symbol.OperationSymbol;
import org.rsrg.semantics.symbol.ProgParameterSymbol;

import java.util.*;

public class ExplicitCallApplicationStrategy
        implements
            StatRuleApplicationStrategy<VCRuleBackedStat> {

    @Override public AssertiveBlock applyRule(
            VCAssertiveBlockBuilder block, VCRuleBackedStat stat) {
        PApply callExp = (PApply) stat.getStatComponents().get(0);
        ExplicitCallRuleApplyingListener applier =
                new ExplicitCallRuleApplyingListener(block);
        callExp.accept(applier);

        return block.finalConfirm(applier.getCompletedExp())
                .snapshot();
    }

    @Override public String getDescription() {
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
        catch (NoSuchSymbolException|DuplicateSymbolException e) {
            //shouldn't happen; well, depends on s.
            throw new RuntimeException(e);
        }
    }

    //TODO: Walk through this step by step in a .md file. Then store the .md file in docs/
    public static class ExplicitCallRuleApplyingListener extends PExpListener {
        public Map<PExp, PExp> test = new HashMap<>();
        private final VCAssertiveBlock.VCAssertiveBlockBuilder block;

        public ExplicitCallRuleApplyingListener(
                VCAssertiveBlock.VCAssertiveBlockBuilder block) {
            this.block = block;
        }

        public PExp getCompletedExp() {
            return block.finalConfirm.getConfirmExp().substitute(test);
        }

        @Override public void endPApply(@NotNull PApply e) {
            PApply thisExp = (PApply) e.substitute(test);
            PSymbol name = (PSymbol) e.getFunctionPortion();
            test.clear(); //TODO: hmmmm..
            List<PExp> actuals = thisExp.getArguments();

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
            Map<PExp, PExp> ensuresEqualities = new HashMap<>();

            for (PExp equals : opEnsures.splitIntoConjuncts()) {
                if (equals.isEquality()) {
                    ensuresEqualities.put(equals.getSubExpressions().get(1),
                            equals.getSubExpressions().get(2));
                }
            }
            if (ensuresEqualities.containsKey(e.getFunctionPortion())) {
                intermediateBindings.put(e,
                        ensuresEqualities.get(e.getFunctionPortion()));
            }
            while (formalParamIter.hasNext()) {
                ProgParameterSymbol formal = formalParamIter.next();
                PExp actual = actualParamIter.next();
                if (formal.getMode() == ProgParameterSymbol.ParameterMode.UPDATES) {
                    if (!ensuresEqualities.containsKey(formal.asPSymbol())) {
                        continue;
                    }
                    intermediateBindings.put(actual,
                            ensuresEqualities.get(formal.asPSymbol()));
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
                test.put(exp.getKey(), v);
            }
        }
    }
}
