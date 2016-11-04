package edu.clemson.resolve.vcgen.stats;

import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.application.VCStatRuleApplicationStrategy;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupString;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class VCWhile extends VCRuleBackedStat {

    private final PExp progCondition, maintaining, decreasing;

    /** I want to preserve order of the original change list */
    private final Set<PSymbol> changingVariables = new LinkedHashSet<>();
    private final List<VCRuleBackedStat> body = new ArrayList<>();

    public VCWhile(ParserRuleContext ctx,
                   VCAssertiveBlockBuilder block,
                   VCStatRuleApplicationStrategy apply,
                   PExp condition,
                   PExp maintaining,
                   PExp decreasing,
                   Set<PSymbol> changing,
                   List<VCRuleBackedStat> stmts) {
        super(ctx, block, apply);
        this.progCondition = condition;
        this.maintaining = maintaining;
        this.decreasing = decreasing;
        this.body.addAll(stmts);
    }

    @NotNull
    public PExp getInvariant() {
        return maintaining;
    }

    @Nullable
    public PExp getDecreasing() {
        return decreasing;
    }

    @NotNull
    public List<VCRuleBackedStat> getBody() {
        return body;
    }

    @NotNull
    public Set<PSymbol> getChangingVariables() {
        return changingVariables;
    }

    @NotNull
    public PExp getProgCondition() {
        return progCondition;
    }

    @NotNull
    @Override
    public VCRuleBackedStat copyWithEnclosingBlock(@NotNull VCAssertiveBlockBuilder b) {
        return new VCWhile(definingCtx, b, applicationStrategy,
                progCondition, maintaining, decreasing,
                changingVariables, Utils.apply(body, e -> e.copyWithEnclosingBlock(b)));
    }

    @Override
    public String toString() {
        STGroup g = new STGroupString("WhileStmt(condition, maintaining, decreasing, body) ::= " +
                "<<While <condition> \n" +
                "    maintaining <maintaining>;\n" +
                "    decreasing <decreasing>;\n" +
                "do\n" +
                "    <body; separator=\"\n\">\n" +
                "end;>>");
        ST t = g.getInstanceOf("WhileStmt");
        t.add("condition", progCondition);
        t.add("maintaining", maintaining);
        t.add("decreasing", decreasing);
        t.add("body", body);
        return t.render();
    }
}
