package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.parser.ResolveParser;
import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.semantics.*;
import edu.clemson.resolve.semantics.query.MathSymbolQuery;
import edu.clemson.resolve.semantics.symbol.MathClssftnWrappingSymbol;
import edu.clemson.resolve.vcgen.AssertiveBlock;
import edu.clemson.resolve.vcgen.VCAssertiveBlock;
import edu.clemson.resolve.vcgen.VCGenerator;
import edu.clemson.resolve.vcgen.stats.VCConfirm;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.stats.VCIfElse;
import edu.clemson.resolve.vcgen.stats.VCWhile;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;

public class WhileApplicationStrategy implements VCStatRuleApplicationStrategy<VCWhile> {

    @NotNull
    @Override
    public AssertiveBlock applyRule(@NotNull VCAssertiveBlockBuilder block, @NotNull VCWhile stat) {
        ResolveParser.WhileStmtContext whileNode = (ResolveParser.WhileStmtContext) stat.getDefiningContext();

        //TODO: Look into this crap where the confirm needs a ctx...maybe it's needed.. can't remember..
        PExp decreasing = stat.getDecreasing();
        PExp invariantAndProgressAssumption = stat.getInvariant();

        //C :: code; Confirm Invariant
        block.confirm(whileNode.maintainingClause(),
                stat.getInvariant().withVCInfo(whileNode.maintainingClause().getStart(),
                        "While loop invariant base case"));

        if (decreasing != null) {
            PSymbol pVal = createPVal(block.g, block.scope);
            PExp nqvPVal = VCGenerator.NPV(block.finalConfirm.getConfirmExp(), pVal);
            invariantAndProgressAssumption =
                    block.g.formConjunct(invariantAndProgressAssumption, block.g.formEquals(nqvPVal, pVal));
        }
        block.assume(invariantAndProgressAssumption);

        ConditionalApplicationStrategy strategy = stat.branchSatisfied() ?
                VCGenerator.IF_APPLICATION : VCGenerator.ELSE_APPLICATION;
        VCIfElse s = new VCIfElse(block.definingTree, block, strategy, stat.getBody(), stat.getProgCondition());

        block.confirm(block.definingTree, block.g.getTrueExp());
        return block.snapshot();
    }

    //TODO: Eventually could have a throws clause... to warn the user that card (or nat) was unable to be found...
    private PSymbol createPVal(DumbMathClssftnHandler g, Scope s) {
        PSymbol.PSymbolBuilder pValBuilder = new PSymbol.PSymbolBuilder("P_Val")
                .mathClssfctn(g.INVALID);   //by default...
        try {   //try to make it a nat though (if it can find it..)
            MathClssftnWrappingSymbol nat = s.queryForOne(new MathSymbolQuery(null, "N"));
            pValBuilder.mathClssfctn(nat.getClassification());
        } catch (NoSuchSymbolException|DuplicateSymbolException|NoSuchModuleException|UnexpectedSymbolException e) {
            //TODO: Perhaps throw a message here warning that PVal couldn't be properly formed (not enough information
            //readily available in scope...)
        }
        return pValBuilder.build();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "While rule application";
    }
}
