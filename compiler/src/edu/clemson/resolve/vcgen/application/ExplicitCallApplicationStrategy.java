package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.parser.ResolveLexer;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.vcgen.BasicBetaReducingListener;
import edu.clemson.resolve.vcgen.FlexibleNameSubstitutingListener;
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
      /*  PSymbol callExp = (PSymbol) stat.getStatComponents().get(0);

        PExpSomethingListener something = new PExpSomethingListener(block);
        callExp.accept(something);

        PExp finalConfirm = block.finalConfirm.getConfirmExp();
        //finalConfirm = finalConfirm.substitute(something.test);
        FlexibleNameSubstitutingListener l =
                new FlexibleNameSubstitutingListener(
                        finalConfirm, something.test);
        finalConfirm.accept(l);
        finalConfirm = l.getSubstitutedExp();
        BasicBetaReducingListener b =
                new BasicBetaReducingListener(something.test, finalConfirm);
        finalConfirm.accept(b);
        finalConfirm = b.getBetaReducedExp();*/
        //return block.finalConfirm(finalConfirm).snapshot();
        return block.snapshot();
    }

   /* protected static OperationSymbol getOperation(Scope s, PSymbol app) {
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
    }*/

    @Override public String getDescription() {
        return "explicit (simple) call rule application";
    }
}
