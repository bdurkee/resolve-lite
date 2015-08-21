package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpListener;
import edu.clemson.resolve.proving.absyn.PLambda;
import edu.clemson.resolve.proving.absyn.PSymbol;

import java.util.HashMap;
import java.util.Map;

/** A visitor that replaces conceptual variables with their correspondence
 *  defined mathematical counterparts.
 */
public class CorrespondenceReducingListener extends PExpListener {

    //'conc.P.Trmnl_Loc' -> 'SS(k)(P.Length, Cen(k))'
    //'conc.P.Curr_Loc' -> 'SS(k)(P.Curr_Place, Cen(k))'
    //'conc.P.Lab' -> \ 'q : Sp_Loc(k).({P.labl.Valu(SCD(q)) if SCD(q) + 1 <= P.Length; ...});'
    private final Map<PExp, PExp> substitutions = new HashMap<>();
    private PExp startingExp;

    public CorrespondenceReducingListener(PExp correspondenceExp, PExp start) {
        for (PExp e : correspondenceExp.splitIntoConjuncts()) {
            PSymbol left = (PSymbol)e.getSubExpressions().get(0);
            substitutions.put(left, e.getSubExpressions().get(1));
        }
        BasicBetaReducingListener r = betaReduce(substitutions, start);
        this.substitutions.putAll(r.substitutions);
        this.startingExp = r.betaReducedExp;
    }

    public CorrespondenceReducingListener(Map<PExp, PExp> substitutions,
                                          PExp start) {
        BasicBetaReducingListener r = betaReduce(substitutions, start);
        this.substitutions.putAll(substitutions);
        this.startingExp = r.betaReducedExp;
    }

    private BasicBetaReducingListener betaReduce(Map<PExp, PExp> substitutions,
                                                PExp startingExp) {
        BasicBetaReducingListener r =
                new BasicBetaReducingListener(substitutions, startingExp);
        startingExp.accept(r);
        return r;
    }

    public PExp getReducedExp() {
        return startingExp;
    }

    @Override public void endPSymbol(PSymbol e) {
        if (substitutions.containsKey(e)) {
            startingExp = startingExp.substitute(e, substitutions.get(e));
        }
    }

    /** Doesn't handle the many of intricacies involved in performing a full,
     *  'canonical' beta-reduction such as alpha renaming, etc. Hence the
     *  'basic' in the name.
     */
    private static class BasicBetaReducingListener extends PExpListener {

        public final Map<PExp, PExp> substitutions;
        public PExp betaReducedExp;

        public BasicBetaReducingListener(Map<PExp, PExp> substitutions,
                                         PExp startingExp) {
            this.substitutions = substitutions;
            this.betaReducedExp = startingExp;
        }

        @Override public void endPSymbol(PSymbol e) {
            if (e.isFunctionApplication() && e.getName().startsWith("conc.")) {
                PSymbol asPlainFunction =   //enables error check
                        new PSymbol.PSymbolBuilder(e.getName())
                                .mathType(e.getMathType())
                                .mathTypeValue(e.getMathTypeValue())
                                .incoming(e.isIncoming()).build();
                if (substitutions.get(asPlainFunction) != null) {
                    PLambda l = (PLambda) substitutions.get(asPlainFunction);
                    PExp newBody = l.getBody().substitute(l.getParametersAsPExps(),
                            e.getArguments());
                    betaReducedExp = betaReducedExp.substitute(e, newBody);
                }
            }
        }
    }
}
