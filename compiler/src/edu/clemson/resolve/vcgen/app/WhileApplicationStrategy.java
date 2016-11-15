package edu.clemson.resolve.vcgen.app;

import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.ResolveParser;
import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.semantics.*;
import edu.clemson.resolve.semantics.query.MathSymbolQuery;
import edu.clemson.resolve.semantics.symbol.MathClssftnWrappingSymbol;
import edu.clemson.resolve.vcgen.VCAssertiveBlock;
import edu.clemson.resolve.vcgen.VCGen;
import edu.clemson.resolve.vcgen.stats.*;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class WhileApplicationStrategy implements RuleApplicationStrategy<VCWhile> {

    @NotNull
    @Override
    public VCAssertiveBlock applyRule(@NotNull Deque<VCAssertiveBlockBuilder> branches,
                                      @NotNull VCAssertiveBlockBuilder block,
                                      @NotNull VCWhile stat) {
        ResolveParser.WhileStmtContext whileNode = (ResolveParser.WhileStmtContext) stat.getDefiningContext();
        //TODO: Look into this crap where the confirm needs a ctx...maybe it's needed.. can't remember..
        PExp decreasing = stat.getDecreasing();
        PExp invariant = stat.getInvariant();

        //C :: code; Confirm Invariant
        if (!stat.getInvariant().equals(block.g.getTrueExp())) {
            block.confirm(whileNode.maintainingClause(),
                    stat.getInvariant().withVCInfo(whileNode.maintainingClause().getStart(),
                            "While loop invariant base case"));
        }

        List<VCRuleBackedStat> thenStmts = Utils.apply(stat.getBody(), e->e.copyWithEnclosingBlock(block));
        List<VCRuleBackedStat> elseStmts = new ArrayList<>();
        PSymbol pVal = createPVal(block.g, block.scope);
        PExp nqvPVal = VCGen.NPV(block.finalConfirm.getSequents(), pVal);

        if (whileNode.changingClause() != null) {
            block.stats(new VCChanging(whileNode.changingClause(), block, stat.getChangingVariables()));
        }
        //Assume the invariant (stipulate) -- the false indicates this isn't a notice...
        if (!invariant.isLiteralTrue()) {
            block.assume(invariant, true, false);
        }

        //Assume the specificational substitution for P_Val = ...
        block.assume(block.g.formEquals(nqvPVal, decreasing));

        //decreasing < nqvPVal
        MathClssftn nat = getNat(block.g, block.scope);
        PSymbol plus = new PSymbol.PSymbolBuilder("+")
                .mathClssfctn(new MathFunctionClssftn(block.g, nat, nat, nat))
                .build();
        PSymbol one = new PSymbol.PSymbolBuilder("1")
                .mathClssfctn(nat)
                .literal(true)
                .build();
        //decreasingExp + 1
        PApply decreasingPlusOne = new PApply.PApplyBuilder(plus)
                .applicationType(nat)
                .arguments(decreasing, one)
                .style(PApply.DisplayStyle.INFIX)
                .build();

        //decreasingExp + 1 ≤ NQV(P_Val)
        PSymbol lte = new PSymbol.PSymbolBuilder("≤")
                .mathClssfctn(new MathFunctionClssftn(block.g, block.g.BOOLEAN, nat, nat))
                .build();
        PApply progressClaimExp = new PApply.PApplyBuilder(lte)
                .applicationType(block.g.BOOLEAN)
                .style(PApply.DisplayStyle.INFIX)
                .arguments(decreasingPlusOne, nqvPVal)
                .build();

        //Confirm Inv /\ P_Exp < NPV(RP, P_Val);
        PExp terminationMetricExp = block.g.formConjunct(invariant, progressClaimExp)
                .withVCInfo(whileNode.decreasingClause().getStart(), "While loop termination");

        VCConfirm terminationConfirm = new VCConfirm(block.definingTree, block,
                VCAssertiveBlock.sequentFormRight(terminationMetricExp));
        thenStmts.add(terminationConfirm);

        //Confirm RP;
        elseStmts.add(block.finalConfirm.copyWithEnclosingBlock(block));

        block.stats(new VCIfElse(stat.getDefiningContext(), block, thenStmts, elseStmts, stat.getProgCondition()));
        block.finalConfirm(block.g.getTrueExp().withVCInfo(whileNode.getStart(), "While loop termination"));

        return block.snapshot();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "While rule application";
    }

    //TODO: Eventually could have a throws clause... to warn the user that card (or nat) was unable to be found...
    private PSymbol createPVal(DumbMathClssftnHandler g, Scope s) {
        return new PSymbol.PSymbolBuilder("P_Val")
                .mathClssfctn(getNat(g, s))
                .build();
    }

    //TODO: Eventually could have a throws clause... to warn the user that card (or nat) was unable to be found...
    private MathClssftn getNat(DumbMathClssftnHandler g, Scope s) {
        try {   //try to make it a nat though (if it can find it..)
            MathClssftnWrappingSymbol nat = s.queryForOne(new MathSymbolQuery(null, "N"));
            return nat.getClassification();
        } catch (NoSuchSymbolException|DuplicateSymbolException|NoSuchModuleException|UnexpectedSymbolException e) {
            //TODO: Perhaps throw a message here warning that PVal couldn't be properly formed (not enough information
            //readily available in scope...)
        }
        return g.INVALID;
    }
}