package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpListener;
import edu.clemson.resolve.proving.absyn.PLambda;
import edu.clemson.resolve.proving.absyn.PSymbol;

import java.util.HashMap;
import java.util.Map;

/** A visitor that replaces conceptual variables with their correspondence
 *  defined mathematical counterparts.
 *  <p>
 *  Note: This listener assumes the starting expression ({@code start}) has
 *  already had all occurences of its conceptually-bound variables prefixed by 'conc'.</p>
 */
public class BasicBetaReducingListener extends PExpListener {

    //'conc.P.Trmnl_Loc' ~> 'SS(k)(P.Length, Cen(k))'
    //'conc.P.Curr_Loc' ~> 'SS(k)(P.Curr_Place, Cen(k))'
    //'conc.P.Lab' ~> \ 'q : Sp_Loc(k).({P.labl.Valu(SCD(q)) if SCD(q) + 1 <= P.Length; ...});'
    private final Map<PExp, PExp> substitutions = new HashMap<>();
    private PExp betaReducedExp;

    public BasicBetaReducingListener(PExp correspondenceExp, PExp start) {
        for (PExp e : correspondenceExp.splitIntoConjuncts()) {
            substitutions.put(e.getSubExpressions().get(0),
                    e.getSubExpressions().get(1));
        }
        betaReducedExp = start;
    }

    public BasicBetaReducingListener(Map<PExp, PExp> substitutions,
                                     PExp start) {
        this.substitutions.putAll(substitutions);
        this.betaReducedExp = start;
    }

    public PExp getBetaReducedExp() {
        return betaReducedExp;
    }

    @Override public void endPSymbol(PSymbol e) {
        //TODO: Second condition here should be: is e.getName contained in the keyset of our substitution map?
        if (e.isFunctionApplication() &&
                substitutions.containsKey(e.withArgumentsErased()) &&
                substitutions.get(e.withArgumentsErased()) instanceof PLambda) {
            PLambda l = (PLambda) substitutions.get(e.withArgumentsErased());
            PExp newBody = l.getBody().substitute(l.getParametersAsPExps(),
                    e.getArguments());
            betaReducedExp = betaReducedExp.substitute(e, newBody);

        }
    }
}
