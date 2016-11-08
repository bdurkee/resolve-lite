package edu.clemson.resolve.vcgen.stats;

import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.VCGen;
import edu.clemson.resolve.vcgen.app.IfElseApplicationStrategy;
import edu.clemson.resolve.vcgen.app.VCStatRuleApplicationStrategy;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupString;

import java.util.ArrayList;
import java.util.List;

public class VCIfElse extends VCRuleBackedStat {

    private final List<VCRuleBackedStat> thenStmts = new ArrayList<>();
    private final List<VCRuleBackedStat> elseStmts = new ArrayList<>();
    private final PExp progCondition;

    public VCIfElse(ParserRuleContext ctx,
                    VCAssertiveBlockBuilder block,
                    List<VCRuleBackedStat> thenStmts,
                    List<VCRuleBackedStat> elseStmts,
                    PExp progCondition) {
        super(ctx, block, new IfElseApplicationStrategy());
        this.progCondition = progCondition;
        this.thenStmts.addAll(thenStmts);
        this.elseStmts.addAll(elseStmts);
    }

    @NotNull
    public PExp getProgIfCondition() {
        return progCondition;
    }

    @NotNull
    public VCIfElse copyWithEnclosingBlock(@NotNull VCAssertiveBlockBuilder b) {
        return new VCIfElse(definingCtx, b,
                Utils.apply(thenStmts, e -> e.copyWithEnclosingBlock(b)),
                Utils.apply(elseStmts, e -> e.copyWithEnclosingBlock(b)), progCondition);
    }

    @NotNull
    public List<VCRuleBackedStat> getThenStmts() {
        return thenStmts;
    }

    @NotNull
    public List<VCRuleBackedStat> getElseStmts() {
        return elseStmts;
    }

    @Override
    public String toString() {
        STGroup g = new STGroupString("IfElseStmt(condition, ifStats, elseStats) ::= " +
                "<<If <condition> then\n" +
                "    <ifStats; separator=\"\n\">\n" +
                "<if(elseStats)>else\n" +
                "    <elseStats; separator=\"\n\"> <endif>\n\n" +
                "end;>>");
        ST t = g.getInstanceOf("IfElseStmt");
        t.add("condition", progCondition);
        t.add("ifStats", thenStmts);
        t.add("elseStats", elseStmts);
        return t.render();
    }
}
