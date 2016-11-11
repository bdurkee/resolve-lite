package edu.clemson.resolve.vcgen.app;

import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.ResolveLexer;
import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpListener;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.proving.absyn.PSymbol.PSymbolBuilder;
import edu.clemson.resolve.semantics.Scope;
import edu.clemson.resolve.semantics.SymbolTableException;
import edu.clemson.resolve.semantics.query.OperationQuery;
import edu.clemson.resolve.semantics.symbol.OperationSymbol;
import edu.clemson.resolve.semantics.symbol.ProgParameterSymbol;
import edu.clemson.resolve.semantics.symbol.ProgParameterSymbol.ParameterMode;
import edu.clemson.resolve.vcgen.VCAssertiveBlock;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.VCGen;
import edu.clemson.resolve.vcgen.stats.VCCall;
import edu.clemson.resolve.vcgen.stats.VCConfirm;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class GeneralCallApplicationStrategy implements VCStatRuleApplicationStrategy<VCCall> {

    //TODO: this will work for 'nested calls' if we use the invk_cond listener on any evaluates mode arguments.
    @NotNull
    @Override
    public VCAssertiveBlock applyRule(@NotNull Deque<VCAssertiveBlockBuilder> accumulator,
                                      @NotNull VCAssertiveBlockBuilder block,
                                      @NotNull VCCall stat) {
        PApply callExp = (PApply) stat.getProgCallExp();
        OperationSymbol op = getOperation(block.scope, callExp);

        //first applyBackingRule any nested calls passed as arguments to evaluate mode parameters.
       /* for (PExp arg : callExp.getArguments()) {
            ProgParameterSymbol p = formalParamIter.next();
            if (p.getMode() == ParameterMode.EVALUATES) {
                FunctionAssignRuleApplyingListener l = new FunctionAssignRuleApplyingListener(stat.getDefiningContext(), block)
            }
        }*/
        final Set<ParameterMode> distinguishedModes =
                new HashSet<>(Arrays.asList(ParameterMode.UPDATES, ParameterMode.REPLACES,
                        ParameterMode.ALTERS, ParameterMode.CLEARS));

        PSymbol functionName = (PSymbol) callExp.getFunctionPortion();

        PExp newAssume = op.getEnsures();
        List<PExp> formalExps = Utils.apply(op.getParameters(), ProgParameterSymbol::asPSymbol);
        PExp confirmPrecondition = op.getRequires();

        //TODO: Before this happens we need to be sure to apply invk condition listener to any evaluates arguments that are calls...
        confirmPrecondition = confirmPrecondition
                .substitute(formalExps, callExp.getArguments())
                .withVCInfo(block.definingTree.getStart(), "Requires clause of " + functionName.getName());
        block.confirm(stat.getDefiningContext(), confirmPrecondition);
        //^^^^^ Here's the old one:
        //block.confirm(ctx, op.getRequires().substitute(formalExps, e.getArguments()));
            /*for (ProgParameterSymbol p : op.getParameters()) {
                //T1.Constraint(t) /\ T3.Constraint(v) /\ T6.Constraint(y) /\
                //postcondition
                //TODO: Ask about these constraints
                if (distinguishedModes.contains(p.getMode())) {
                    if (p.getDeclaredType() instanceof PTFamily) {
                        newAssume = block.g.formConjunct(newAssume,
                                ((PTFamily) p.getDeclaredType())
                                        .getConstraint());
                    }
                }
            }*/
        VCConfirm currFinalConfirm = block.finalConfirm;
        Map<PExp, PExp> newAssumeSubtitutions = new HashMap<>();
        Iterator<ProgParameterSymbol> formalIter = op.getParameters().iterator();
        Iterator<PExp> argIter = callExp.getArguments().iterator();

        while (formalIter.hasNext()) {
            ProgParameterSymbol curFormal = formalIter.next();
            PExp curActual = (PExp) argIter.next();

            //t ~> NPV(RP, a), @t ~> a
            if (curFormal.getMode() == ParameterMode.UPDATES) {
                newAssumeSubtitutions.put(curFormal.asPSymbol(), VCGen.NPV(currFinalConfirm.getSequents(), (PSymbol) curActual));
                newAssumeSubtitutions.put(new PSymbolBuilder(
                        curFormal.asPSymbol()).incoming(true).build(), (PSymbol) curActual);
            }
            //v ~> NPV(RP, b)
            else if (curFormal.getMode() == ParameterMode.REPLACES) {
                newAssumeSubtitutions.put(curFormal.asPSymbol(), VCGen.NPV(currFinalConfirm.getSequents(), (PSymbol) curActual));
            }
            //@y ~> e, @z ~> f
            else if (curFormal.getMode() == ParameterMode.ALTERS || curFormal.getMode() == ParameterMode.CLEARS) {
                newAssumeSubtitutions.put(new PSymbolBuilder(curFormal.asPSymbol())
                        .incoming(true).build(), curActual);
            }
            else if (curFormal.getMode() == ParameterMode.EVALUATES) {

            }
            else {
                newAssumeSubtitutions.put(curFormal.asPSymbol(), curActual);
            }
        }

        //Assume (T1.Constraint(t) /\ T3.Constraint(v) /\ T6.Constraint(y) /\
        //Post [ t ~> NPV(RP, a), @t ~> a, u ~> Math(exp), v ~> NPV(RP, b),
        //       w ~> c, x ~> d, @y ~> e, @z ~> f]
        block.assume(newAssume.substitute(newAssumeSubtitutions));

        //Ok, so this happens down here since the rule is laid out s.t.
        //substitutions occur prior to conjuncting this -- consult the
        //rule and see for yourself
            /* for (ProgParameterSymbol p : op.getParameters()) {
                //T7.Is_Initial(NPV(RP, f));
                //TODO: See todo above
                if (p.getMode() == CLEARS) {
                    PExp initPred =
                            block.g.formInitializationPredicate(
                                    p.getDeclaredType(), p.getNameToken());
                    newAssume = block.g.formConjunct(newAssume, initPred);
                }
            }*/

        //reset the formal param iter in preperation for building the
        //substitution mapping for our confirm
        formalIter = op.getParameters().iterator();
        argIter = callExp.getArguments().iterator();
        Map<PExp, PExp> confirmSubstitutions = new HashMap<>();
        for (PExp actualArg : callExp.getArguments()) {
            ProgParameterSymbol curFormal = formalIter.next();
            if (distinguishedModes.contains(curFormal.getMode())) {
                confirmSubstitutions.put(actualArg, VCGen.NPV(currFinalConfirm.getSequents(), (PSymbol) actualArg));
            }
        }
        VCConfirm workingConfirm = block.finalConfirm.withSequentFormulaSubstitution(confirmSubstitutions);
        return block.finalConfirm(workingConfirm).snapshot();
    }

    public static OperationSymbol getOperation(Scope s, PApply app) {
        PSymbol name = (PSymbol) app.getFunctionPortion();
        Token qualifier = (name.getQualifier() != null) ? new CommonToken(ResolveLexer.ID, name.getQualifier()) : null;
        try {
            return s.queryForOne(new OperationQuery(qualifier, name.getName(),
                    Utils.apply(app.getArguments(), PExp::getProgType)));
        } catch (SymbolTableException e) {
            //shouldn't happen; well, depends on s.
            throw new RuntimeException(e);
        }
    }

    @NotNull
    @Override
    public String getDescription() {
        return "General call rule application";
    }

}