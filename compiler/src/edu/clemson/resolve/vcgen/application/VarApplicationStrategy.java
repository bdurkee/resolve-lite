package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.parser.ResolveParser;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.semantics.programtype.ProgGenericType;
import edu.clemson.resolve.semantics.programtype.ProgNamedType;
import edu.clemson.resolve.semantics.programtype.ProgType;
import edu.clemson.resolve.vcgen.AssertiveBlock;
import edu.clemson.resolve.vcgen.VCAssertiveBlock;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.stats.VCRuleBackedStat;
import edu.clemson.resolve.vcgen.stats.VCVar;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jetbrains.annotations.NotNull;

import java.util.Deque;
import java.util.Map;

public class VarApplicationStrategy implements VCStatRuleApplicationStrategy<VCVar> {

    @NotNull
    @Override
    public AssertiveBlock applyRule(@NotNull Deque<VCAssertiveBlockBuilder> branches,
                                    @NotNull VCAssertiveBlockBuilder block,
                                    @NotNull VCVar stat) {
        ProgType type = stat.getVarType();
        PSymbol var = new PSymbol.PSymbolBuilder(stat.getName())
                .progType(type).mathClssfctn(type.toMath())
                .build();

        if (type instanceof ProgNamedType) {
            PSymbol exemplar = ((ProgNamedType) type).getExemplarAsPSymbol();
            PExp init = ((ProgNamedType) type).getInitializationEnsures();
            init = init.substitute(exemplar, var);
            ResolveParser.VarDeclGroupContext concreteNode =
                    (ResolveParser.VarDeclGroupContext) stat.getDefiningContext();
            //substitute by the facility the type came through

            //get the qualifier out of the concrete syntax.
            if (concreteNode.type() instanceof ResolveParser.NamedTypeContext) {
                ResolveParser.NamedTypeContext namedTypeNode =
                        (ResolveParser.NamedTypeContext) concreteNode.type();
                Map<PExp, PExp> facilitySubstitutions =
                        block.facilitySpecializations.get(namedTypeNode.qualifier.getText());
                if (namedTypeNode.qualifier != null && facilitySubstitutions != null) {
                    init = init.substitute(facilitySubstitutions);
                }
            }
            block.assume(init);
        }
        else if (type instanceof ProgGenericType) {
            //TODO: handle generics
        }
        return block.snapshot();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Variable rule application";
    }
}
