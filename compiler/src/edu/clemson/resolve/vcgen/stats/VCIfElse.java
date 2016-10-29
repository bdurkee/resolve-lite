package edu.clemson.resolve.vcgen.stats;

import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.VCGenerator;
import edu.clemson.resolve.vcgen.application.ConditionalApplicationStrategy;
import edu.clemson.resolve.vcgen.application.VCStatRuleApplicationStrategy;
import edu.clemson.resolve.vcgen.application.ConditionalApplicationStrategy.IfApplicationStrategy;
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
                    VCStatRuleApplicationStrategy apply,
                    List<VCRuleBackedStat> thenStmts,
                    List<VCRuleBackedStat> elseStmts,
                    PExp progCondition) {
        super(ctx, block, apply);
        this.progCondition = progCondition;
        this.thenStmts.addAll(thenStmts);
        this.elseStmts.addAll(elseStmts);
    }

    @NotNull
    public ConditionalApplicationStrategy getOppositeConditionalStrategy() {
        return applicationStrategy instanceof IfApplicationStrategy ?
                VCGenerator.ELSE_APPLICATION :
                VCGenerator.IF_APPLICATION;
    }

    @NotNull
    public PExp getProgIfCondition() {
        return progCondition;
    }

    @NotNull
    public VCIfElse copyWithEnclosingBlock(@NotNull VCAssertiveBlockBuilder b) {
        return new VCIfElse(definingCtx, b, applicationStrategy,
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

    /*
    @Override
    public String printNested() {
        return printNested("  ");
    }

    protected String printNested( String indentWs) {
        String prevIndentWs = indentWs;
        String result = prevIndentWs.equals("  ") ? "" : indentWs;
        result += "If " + progCondition + " then\n";
        for (VCRuleBackedStat stmt : thenStmts) {
            if (stmt instanceof VCIfElse) {
                VCIfElse stmtAsIfElse = (VCIfElse)stmt;
                indentWs += indentWs;
                result += stmtAsIfElse.printNested(indentWs + indentWs);

                if (!stmtAsIfElse.getElseStmts().isEmpty()) {

                }
            }
            else {
                result += indentWs + stmt + "\n";
            }
        }
        result += "end;";
        return result;
    }*/

    @Override
    public String toString() {
        STGroup g = new STGroupString("IfElseStmt(condition, ifStats, elseStats) ::= " +
                "<<If <condition> then\n" +
                "    <ifStats; separator=\"\n\">\n" +
                "end;>>");
        ST t = g.getInstanceOf("IfElseStmt");
        t.add("condition", progCondition);
        t.add("ifStats", thenStmts);
        t.add("elseStats", elseStmts);
       // x.add("condition", progCondition);
       // x.add("thenStats", thenStmts);
        return t.render();
    }
}
